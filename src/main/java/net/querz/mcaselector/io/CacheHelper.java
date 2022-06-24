package net.querz.mcaselector.io;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.ui.component.TileMapBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;

public final class CacheHelper {

	private static final Logger LOGGER = LogManager.getLogger(CacheHelper.class);

	private CacheHelper() {}

	public static void forceGenerateCache(Integer zoomLevel, Progress progressChannel) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> FileHelper.MCA_FILE_PATTERN.matcher(n).matches());
		if (files == null || files.length == 0) {
			return;
		}

		progressChannel.setMax(files.length);
		progressChannel.updateProgress(files[0].getName(), 0);

		for (File file : files) {
			Matcher m = FileHelper.REGION_GROUP_PATTERN.matcher(file.getName());
			if (m.find()) {
				int x = Integer.parseInt(m.group("regionX"));
				int z = Integer.parseInt(m.group("regionZ"));
				RegionImageGenerator.generate(new Tile(new Point2i(x, z)), (i, u) -> {}, zoomLevel, progressChannel, false, null);
			}
		}
	}

	public static void clearAllCache(TileMap tileMap) {
		JobHandler.cancelAllJobsAndFlush();
		for (File cacheDir : Config.getCacheDirs()) {
			FileHelper.deleteDirectory(cacheDir);
		}
		RegionImageGenerator.invalidateCachedMCAFiles();
		updateVersionFile();
		updateWorldSettingsFile();
		tileMap.clear();
		tileMap.draw();
	}

	// asynchronously cancels all jobs and marks all Tiles as "not loaded"
	// but doesn't remove their images from memory.
	public static void clearAllCacheAsync(TileMap tileMap, Runnable callback) {
		Thread clear = new Thread(() -> {
			JobHandler.cancelAllJobsAndFlush();
			for (File cacheDir : Config.getCacheDirs()) {
				FileHelper.deleteDirectory(cacheDir);
			}
			updateVersionFile();
			updateWorldSettingsFile();

			tileMap.markAllTilesAsObsolete();
			tileMap.draw();
			callback.run();
		});
		clear.start();
	}

	public static void clearViewCache(TileMap tileMap) {
		for (Point2i region : tileMap.getVisibleRegions()) {
			for (File cacheDir : Config.getCacheDirs()) {
				File file = FileHelper.createPNGFilePath(cacheDir, region);
				if (file.exists()) {
					if (!file.delete()) {
						LOGGER.warn("could not delete file {}", file);
					}
				}
				tileMap.clearTile(region.asLong());
				tileMap.getOverlayPool().discardData(region);
			}
		}
		RegionImageGenerator.invalidateCachedMCAFiles();
		tileMap.draw();
	}

	public static void clearSelectionCache(TileMap tileMap) {
		Selection selection = tileMap.getSelection().getTrueSelection(Config.getWorldDirs());

		for (Long2ObjectMap.Entry<ChunkSet> entry : selection) {
			Point2i region = new Point2i(entry.getLongKey());
			for (File cacheDir : Config.getCacheDirs()) {
				File file = FileHelper.createPNGFilePath(cacheDir, region);
				if (file.exists()) {
					if (!file.delete()) {
						LOGGER.warn("could not delete file {}", file);
					}
				}
			}
			tileMap.clearTile(entry.getLongKey());
			tileMap.getOverlayPool().discardData(region);
		}
		RegionImageGenerator.invalidateCachedMCAFiles();
		tileMap.draw();
	}

	public static void validateCacheVersion(TileMap tileMap) {
		String applicationVersion = null;
		try {
			applicationVersion = FileHelper.getManifestAttributes().getValue("Application-Version");
		} catch (IOException ex) {
			// do nothing
		}

		// if we are not running from a .jar, we won't touch the cache files
		if (applicationVersion == null) {
			LOGGER.warn("failed to fetch application version");
			return;
		}

		File cacheVersionFile = new File(Config.getCacheDir(), "version");
		String version = null;
		if(cacheVersionFile.exists()) {
			version = readVersionFromFile(cacheVersionFile);
		} else {
			LOGGER.warn("no cache found for this world");
		}
		if (!applicationVersion.equals(version)) {
			clearAllCache(tileMap);
		}
	}

	private static String readVersionFromFile(File file) {
		String version = null;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			version = br.readLine();
		} catch (IOException ex) {
			LOGGER.warn("failed to read version from file {}", file, ex);
		}
		return version;
	}

	public static void updateVersionFile() {
		String applicationVersion = null;
		try {
			applicationVersion = FileHelper.getManifestAttributes().getValue("Application-Version");
		} catch (IOException ex) {
			// do nothing
		}

		if (applicationVersion == null) {
			LOGGER.warn("no application version found, not updating cache version");
			return;
		}

		File versionFile = new File(Config.getCacheDir(), "version");
		if (!versionFile.getParentFile().exists()) {
			if (!versionFile.getParentFile().mkdirs()) {
				LOGGER.warn("failed to create directory for {}", versionFile);
			}
		}

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(versionFile))) {
			bw.write(applicationVersion);
		} catch (IOException ex) {
			LOGGER.warn("failed to write cache version file", ex);
		}
	}

	public static void readWorldSettingsFile(TileMap tileMap) {
		File worldSettingsFile = new File(Config.getCacheDir(), "world_settings.json");
		if (!worldSettingsFile.exists()) {
			return;
		}

		String poi = null;
		String entities = null;
		int height = Config.DEFAULT_RENDER_HEIGHT;
		boolean layerOnly = Config.DEFAULT_RENDER_LAYER_ONLY;
		boolean caves = Config.DEFAULT_RENDER_CAVES;
		boolean shade = Config.DEFAULT_SHADE;
		boolean shadeWater = Config.DEFAULT_SHADE_WATER;
		boolean smooth = Config.DEFAULT_SMOOTH_RENDERING;
		boolean smoothOverlays = Config.DEFAULT_SMOOTH_OVERLAYS;
		String tileMapBackground = Config.DEFAULT_TILEMAP_BACKGROUND;
		boolean showNonexistentRegions = Config.DEFAULT_SHOW_NONEXISTENT_REGIONS;

		try {
			byte[] data = Files.readAllBytes(worldSettingsFile.toPath());
			JSONObject root = new JSONObject(new String(data));
			poi = root.has("poi") ? root.getString("poi") : null;
			entities = root.has("entities") ? root.getString("entities") : null;
			height = root.has("height") ? root.getInt("height") : Config.DEFAULT_RENDER_HEIGHT;
			layerOnly = root.has("layerOnly") ? root.getBoolean("layerOnly") : Config.DEFAULT_RENDER_LAYER_ONLY;
			caves = root.has("caves") ? root.getBoolean("caves") : Config.DEFAULT_RENDER_CAVES;
			shade = root.has("shade") ? root.getBoolean("shade") : Config.DEFAULT_SHADE;
			shadeWater = root.has("shadeWater") ? root.getBoolean("shadeWater") : Config.DEFAULT_SHADE_WATER;
			smooth = root.has("smooth") ? root.getBoolean("smooth") : Config.DEFAULT_SMOOTH_RENDERING;
			smoothOverlays = root.has("smoothOverlays") ? root.getBoolean("smoothOverlays") : Config.DEFAULT_SMOOTH_OVERLAYS;
			tileMapBackground = root.has("tileMapBackground") ? root.getString("tileMapBackground") : Config.DEFAULT_TILEMAP_BACKGROUND;
			showNonexistentRegions = root.has("showNonexistentRegions") ? root.getBoolean("showNonexistentRegions") : Config.DEFAULT_SHOW_NONEXISTENT_REGIONS;
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		if (poi != null && !poi.isEmpty() && !poi.equals("null")) {
			Config.getWorldDirs().setPoi(new File(poi));
		}
		if (entities != null && !entities.isEmpty() && !entities.equals("null")) {
			Config.getWorldDirs().setEntities(new File(entities));
		}
		Config.setRenderHeight(height);
		Config.setRenderLayerOnly(layerOnly);
		Config.setRenderCaves(caves);
		Config.setShade(shade);
		Config.setShadeWater(shadeWater);
		Config.setSmoothRendering(smooth);
		Config.setSmoothOverlays(smoothOverlays);
		Config.setTileMapBackground(tileMapBackground);
		try {
			tileMap.getWindow().getTileMapBox().setBackground(TileMapBox.TileMapBoxBackground.valueOf(tileMapBackground).getBackground());
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
		}
		Config.setShowNonExistentRegions(showNonexistentRegions);

		LOGGER.debug(Config.asString());
	}

	public static void updateWorldSettingsFile() {
		File worldSettingsFile = new File(Config.getCacheDir(), "world_settings.json");
		if (!worldSettingsFile.getParentFile().exists()) {
			if (!worldSettingsFile.getParentFile().mkdirs()) {
				LOGGER.warn("failed to create directory for {}", worldSettingsFile);
			}
		}

		JSONObject root = new JSONObject();
		root.put("poi", Config.getWorldDirs().getPoi());
		root.put("entities", Config.getWorldDirs().getEntities());
		root.put("height", Config.getRenderHeight());
		root.put("layerOnly", Config.renderLayerOnly());
		root.put("caves", Config.renderCaves());
		root.put("shade", Config.shade());
		root.put("shadeWater", Config.shadeWater());
		root.put("smooth", Config.smoothRendering());
		root.put("smoothOverlays", Config.smoothOverlays());
		root.put("tileMapBackground", Config.getTileMapBackground());
		root.put("showNonexistentRegions", Config.showNonExistentRegions());

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(worldSettingsFile))) {
			bw.write(root.toString());
		} catch (IOException ex) {
			LOGGER.warn("failed to write world settings file", ex);
		}
	}
}
