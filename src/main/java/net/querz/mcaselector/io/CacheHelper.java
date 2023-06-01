package net.querz.mcaselector.io;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.progress.Progress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;

public final class CacheHelper {

	private static final Logger LOGGER = LogManager.getLogger(CacheHelper.class);

	private CacheHelper() {}

	public static void forceGenerateCache(Integer zoomLevel, Progress progressChannel) {
		File[] files = ConfigProvider.WORLD.getRegionDir().listFiles((d, n) -> FileHelper.MCA_FILE_PATTERN.matcher(n).matches());
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
		for (File cacheDir : ConfigProvider.WORLD.getCacheDirs()) {
			FileHelper.deleteDirectory(cacheDir);
		}
		RegionImageGenerator.invalidateCachedMCAFiles();
		updateVersionFile();
		ConfigProvider.WORLD.save();
		tileMap.clear();
		tileMap.draw();
	}

	// asynchronously cancels all jobs and marks all Tiles as "not loaded"
	// but doesn't remove their images from memory.
	public static void clearAllCacheAsync(TileMap tileMap, Runnable callback) {
		Thread clear = new Thread(() -> {
			JobHandler.cancelAllJobsAndFlush();
			for (File cacheDir : ConfigProvider.WORLD.getCacheDirs()) {
				FileHelper.deleteDirectory(cacheDir);
			}
			updateVersionFile();
			ConfigProvider.WORLD.save();

			tileMap.markAllTilesAsObsolete();
			tileMap.draw();
			callback.run();
		});
		clear.start();
	}

	public static void clearViewCache(TileMap tileMap) {
		for (Point2i region : tileMap.getVisibleRegions()) {
			for (File cacheDir : ConfigProvider.WORLD.getCacheDirs()) {
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
		Selection selection = tileMap.getSelection().getTrueSelection(ConfigProvider.WORLD.getWorldDirs());

		for (Long2ObjectMap.Entry<ChunkSet> entry : selection) {
			Point2i region = new Point2i(entry.getLongKey());
			for (File cacheDir : ConfigProvider.WORLD.getCacheDirs()) {
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

		File cacheVersionFile = new File(ConfigProvider.WORLD.getCacheDir(), "version");
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

		File versionFile = new File(ConfigProvider.WORLD.getCacheDir(), "version");
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
}
