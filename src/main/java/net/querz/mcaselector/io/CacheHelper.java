package net.querz.mcaselector.io;

import javafx.application.Platform;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.progress.Progress;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public final class CacheHelper {

	private CacheHelper() {}

	public static void forceGenerateCache(Integer zoomLevel, Progress progressChannel) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> n.matches(FileHelper.MCA_FILE_PATTERN));
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
				boolean scaleOnly = zoomLevel != null;
				float zoomLevelSupplier = scaleOnly ? zoomLevel : 1;
				RegionImageGenerator.generate(new Tile(new Point2i(x, z)), (i, u) -> {}, () -> zoomLevelSupplier, scaleOnly, progressChannel, false);
			}
		}
	}

	public static void clearAllCache(TileMap tileMap) {
		MCAFilePipe.cancelAllJobsAndFlush();
		for (File cacheDir : Config.getCacheDirs()) {
			FileHelper.deleteDirectory(cacheDir);
		}
		updateVersionFile();
		updateWorldSettingsFile();
		RegionImageGenerator.invalidateCachedMCAFiles();
		tileMap.clear();
		tileMap.update();
	}

	// asynchronously cancels all jobs and marks all Tiles as "not loaded"
	// but doesn't remove their images from memory.
	public static void clearAllCacheAsync(TileMap tileMap, Runnable callback) {
		Thread clear = new Thread(() -> {
			MCAFilePipe.cancelAllJobsAndFlush();
			for (File cacheDir : Config.getCacheDirs()) {
				FileHelper.deleteDirectory(cacheDir);
			}
			updateVersionFile();
			updateWorldSettingsFile();

			Platform.runLater(() -> {
				tileMap.markAllTilesAsObsolete();
				tileMap.update();
			});
			callback.run();
		});
		clear.start();
	}

	public static void clearViewCache(TileMap tileMap) {
		for (Point2i regionBlock : tileMap.getVisibleRegions()) {
			for (File cacheDir : Config.getCacheDirs()) {
				File file = FileHelper.createPNGFilePath(cacheDir, regionBlock);
				if (file.exists()) {
					if (!file.delete()) {
						Debug.error("could not delete file " + file);
					}
				}
				tileMap.clearTile(regionBlock);
				tileMap.getOverlayPool().discardData(regionBlock);
			}
		}
		RegionImageGenerator.invalidateCachedMCAFiles();
		tileMap.update();
	}

	public static void clearSelectionCache(TileMap tileMap) {
		if (tileMap.isSelectionInverted()) {
			SelectionData selection = new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted());
			File[] cacheDirs = Config.getCacheDirs();
			for (File cacheDir : cacheDirs) {
				File[] cacheFiles = cacheDir.listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.png$"));
				if (cacheFiles == null) {
					continue;
				}
				for (File cacheFile : cacheFiles) {
					Point2i cacheRegion = FileHelper.parseCacheFileName(cacheFile);
					if (selection.isRegionSelected(cacheRegion) && cacheFile.exists()) {
						if (!cacheFile.delete()) {
							Debug.error("could not delete file " + cacheFile);
							continue;
						}
						tileMap.clearTile(cacheRegion);
					}
				}
			}

			Map<Point2i, Set<Point2i>> trueSelection = SelectionHelper.getTrueSelection(selection);
			for (Point2i region : trueSelection.keySet()) {
				tileMap.getOverlayPool().discardData(region);
			}
		} else {
			for (Map.Entry<Point2i, Set<Point2i>> entry : tileMap.getMarkedChunks().entrySet()) {
				for (File cacheDir : Config.getCacheDirs()) {
					File file = FileHelper.createPNGFilePath(cacheDir, entry.getKey());
					if (file.exists()) {
						if (!file.delete()) {
							Debug.error("could not delete file " + file);
						}
					}
					tileMap.clearTile(entry.getKey());
				}
				tileMap.getOverlayPool().discardData(entry.getKey());

			}
		}
		RegionImageGenerator.invalidateCachedMCAFiles();
		tileMap.update();
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
			Debug.dump("failed to fetch application version");
			return;
		}

		File cacheVersionFile = new File(Config.getCacheDir(), "version");

		String version = readVersionFromFile(cacheVersionFile);
		if (!applicationVersion.equals(version)) {
			clearAllCache(tileMap);
		}
	}

	private static String readVersionFromFile(File file) {
		String version = null;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			version = br.readLine();
		} catch (IOException ex) {
			Debug.dumpException("failed to read version from file " + file, ex);
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
			Debug.dump("no application version found, not updating cache version");
			return;
		}

		File versionFile = new File(Config.getCacheDir(), "version");
		if (!versionFile.getParentFile().exists()) {
			versionFile.getParentFile().mkdirs();
		}

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(versionFile))) {
			bw.write(applicationVersion);
		} catch (IOException ex) {
			Debug.dumpException("failed to write cache version file", ex);
		}
	}

	public static void readWorldSettingsFile() {
		File worldSettingsFile = new File(Config.getCacheDir(), "world_settings.json");
		if (!worldSettingsFile.exists()) {
			return;
		}

		String poi = null;
		String entities = null;
		int height = Config.DEFAULT_RENDER_HEIGHT;
		boolean layerOnly = Config.DEFAULT_RENDER_LAYER_ONLY;

		try {
			byte[] data = Files.readAllBytes(worldSettingsFile.toPath());
			JSONObject root = new JSONObject(new String(data));
			poi = root.has("poi") ? root.getString("poi") : null;
			entities = root.has("entities") ? root.getString("entities") : null;
			height = root.has("height") ? root.getInt("height") : Config.DEFAULT_RENDER_HEIGHT;
			layerOnly = root.has("layerOnly") ? root.getBoolean("layerOnly") : Config.DEFAULT_RENDER_LAYER_ONLY;
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (poi != null && !poi.isEmpty() && !poi.equals("null")) {
			Config.getWorldDirs().setPoi(new File(poi));
		}
		if (entities != null && !entities.isEmpty() && !entities.equals("null")) {
			Config.getWorldDirs().setEntities(new File(entities));
		}
		Config.setRenderHeight(height);
		Config.setRenderLayerOnly(layerOnly);
	}

	public static void updateWorldSettingsFile() {
		File worldSettingsFile = new File(Config.getCacheDir(), "world_settings.json");
		if (!worldSettingsFile.getParentFile().exists()) {
			worldSettingsFile.getParentFile().mkdirs();
		}

		JSONObject root = new JSONObject();
		root.put("poi", Config.getWorldDirs().getPoi());
		root.put("entities", Config.getWorldDirs().getEntities());
		root.put("height", Config.getRenderHeight());
		root.put("layerOnly", Config.renderLayerOnly());

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(worldSettingsFile))) {
			bw.write(root.toString());
		} catch (IOException ex) {
			Debug.dumpException("failed to write world settings file", ex);
		}
	}
}
