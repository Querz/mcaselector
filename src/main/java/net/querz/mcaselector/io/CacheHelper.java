package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.progress.Progress;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
				RegionImageGenerator.generate(new Tile(new Point2i(x, z)), null, (i, u) -> {}, () -> zoomLevelSupplier, scaleOnly, progressChannel);
			}
		}
	}

	public static void clearAllCache(TileMap tileMap) {
		for (File cacheDir : Config.getCacheDirs()) {
			File[] files = cacheDir.listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.png$"));
			if (files != null) {
				for (File file : files) {
					if (!file.isDirectory()) {
						Debug.dump("deleting " + file);
						if (!file.delete()) {
							Debug.error("could not delete file " + file);
						}
					}
				}
			}
		}
		updateVersionFile();
		tileMap.clear();
		tileMap.update();
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
			}
		}
		tileMap.clear();
		tileMap.update();
	}

	public static void clearSelectionCache(TileMap tileMap) {
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
		}
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

	private static void updateVersionFile() {
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
}
