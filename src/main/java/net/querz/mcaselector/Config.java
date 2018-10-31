package net.querz.mcaselector;

import javafx.scene.paint.Color;
import net.querz.mcaselector.util.Debug;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public final class Config {

	public static final File DEFAULT_BASE_CACHE_DIR = new File(System.getProperty("user.dir") + "/cache");
	public static final Color DEFAULT_REGION_SELECTION_COLOR = new Color(1, 0, 0, 0.8);
	public static final Color DEFAULT_CHUNK_SELECTION_COLOR = new Color(1, 0.45, 0, 0.8);
	public static final int DEFAULT_LOAD_THREADS = 1;
	public static final int DEFAULT_PROCESS_THREADS = Runtime.getRuntime().availableProcessors();
	public static final int DEFAULT_WRITE_THREADS = 4;
	public static final int DEFAULT_MAX_LOADED_FILES = DEFAULT_PROCESS_THREADS + (DEFAULT_PROCESS_THREADS / 2);
	public static final boolean DEFAULT_DEBUG = false;

	private static File worldDir = null;
	private static File baseCacheDir = DEFAULT_BASE_CACHE_DIR;
	private static File cacheDir = null;

	private static Color regionSelectionColor = DEFAULT_REGION_SELECTION_COLOR;
	private static Color chunkSelectionColor = DEFAULT_CHUNK_SELECTION_COLOR;
	private static int loadThreads = DEFAULT_LOAD_THREADS;
	private static int processThreads = DEFAULT_PROCESS_THREADS;
	private static int writeThreads = DEFAULT_WRITE_THREADS;
	private static int maxLoadedFiles = DEFAULT_MAX_LOADED_FILES;

	private static boolean debug = DEFAULT_DEBUG;

	private Config() {}

	public static File getWorldDir() {
		return worldDir;
	}

	public static void setWorldDir(File worldDir) {
		Config.worldDir = worldDir;
		UUID uuid = UUID.nameUUIDFromBytes(worldDir.getAbsolutePath().getBytes());
		cacheDir = new File(baseCacheDir, uuid.toString().replace("-", ""));
	}

	public static File getCacheDir() {
		return cacheDir;
	}

	public static void setDebug(boolean debug) {
		Config.debug = debug;
	}

	public static boolean debug() {
		return Config.debug;
	}

	public static Color getRegionSelectionColor() {
		return regionSelectionColor;
	}

	public static void setRegionSelectionColor(Color regionSelectionColor) {
		Config.regionSelectionColor = regionSelectionColor;
	}

	public static void loadFromIni() {
		File file = new File(System.getProperty("user.dir"), "settings.ini");
		if (!file.exists()) {
			return;
		}
		Map<String, String> config = new HashMap<>();
		try {
			Files.lines(file.toPath()).forEach(l -> {
				if (l.charAt(0) == ';') {
					return;
				}
				String[] elements = l.split("=", 2);
				if (elements.length != 2) {
					Debug.errorf("invalid line in settings.ini: \"%s\"", l);
					return;
				}
				config.put(elements[0], elements[1]);
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			//set values
			baseCacheDir = new File(config.getOrDefault(
				"BaseCacheDir",
				baseCacheDir.getAbsolutePath()).replace("{user.dir}", System.getProperty("user.dir"))
			);
			regionSelectionColor = Color.web(config.getOrDefault("RegionSelectionColor", DEFAULT_REGION_SELECTION_COLOR.toString()));
			chunkSelectionColor = Color.web(config.getOrDefault("ChunkSelectionColor", DEFAULT_CHUNK_SELECTION_COLOR.toString()));
			loadThreads = Integer.parseInt(config.getOrDefault("LoadThreads", DEFAULT_LOAD_THREADS + ""));
			processThreads = Integer.parseInt(config.getOrDefault("ProcessThreads", DEFAULT_PROCESS_THREADS + ""));
			writeThreads = Integer.parseInt(config.getOrDefault("WriteThreads", DEFAULT_WRITE_THREADS + ""));
			maxLoadedFiles = Integer.parseInt(config.getOrDefault("MaxLoadedFiles", DEFAULT_MAX_LOADED_FILES + ""));
			debug = Boolean.parseBoolean(config.getOrDefault("Debug", DEFAULT_DEBUG + ""));
		} catch (Exception ex) {
			Debug.errorf("error loading settings.ini: %s", ex.getMessage());
		}
	}

	public static void exportConfig() {
		String userDir = System.getProperty("user.dir");
		File file = new File(userDir, "settings.ini");
		List<String> lines = new ArrayList<>(8);
		addSettingsLine(
			"BaseCacheDir",
			baseCacheDir.getAbsolutePath().replace(userDir, "{user.dir}"),
			DEFAULT_BASE_CACHE_DIR.getAbsolutePath().replace(userDir, "{user.dir}"), lines);
		addSettingsLine("RegionSelectionColor", regionSelectionColor.toString(), DEFAULT_REGION_SELECTION_COLOR.toString(), lines);
		addSettingsLine("ChunkSelectionColor", chunkSelectionColor.toString(), DEFAULT_CHUNK_SELECTION_COLOR.toString(), lines);
		addSettingsLine("LoadThreads", loadThreads, DEFAULT_LOAD_THREADS, lines);
		addSettingsLine("ProcessThreads", processThreads, DEFAULT_PROCESS_THREADS, lines);
		addSettingsLine("WriteThreads", writeThreads, DEFAULT_WRITE_THREADS, lines);
		addSettingsLine("MaxLoadedFiles", maxLoadedFiles, DEFAULT_MAX_LOADED_FILES, lines);
		addSettingsLine("Debug", debug, DEFAULT_DEBUG, lines);
		if (lines.size() == 0) {
			if (file.exists() && !file.delete()) {
				Debug.errorf("could not delete %s", file.getAbsolutePath());
			}
			return;
		}
		try {
			Files.write(file.toPath(), lines);
		} catch (IOException ex) {
			Debug.errorf("error writing settings.ini: %s", ex.getMessage());
		}
	}

	private static void addSettingsLine(String key, Object value, Object def, List<String> lines) {
		if (!value.equals(def)) {
			lines.add(key + "=" + value);
		}
	}

	public static Color getChunkSelectionColor() {
		return chunkSelectionColor;
	}

	public static void setChunkSelectionColor(Color chunkSelectionColor) {
		Config.chunkSelectionColor = chunkSelectionColor;
	}

	public static int getLoadThreads() {
		return loadThreads;
	}

	public static void setLoadThreads(int loadThreads) {
		Config.loadThreads = loadThreads;
	}

	public static int getProcessThreads() {
		return processThreads;
	}

	public static void setProcessThreads(int processThreads) {
		Config.processThreads = processThreads;
	}

	public static int getWriteThreads() {
		return writeThreads;
	}

	public static void setWriteThreads(int writeThreads) {
		Config.writeThreads = writeThreads;
	}

	public static int getMaxLoadedFiles() {
		return maxLoadedFiles;
	}

	public static void setMaxLoadedFiles(int maxLoadedFiles) {
		Config.maxLoadedFiles = maxLoadedFiles;
	}
}
