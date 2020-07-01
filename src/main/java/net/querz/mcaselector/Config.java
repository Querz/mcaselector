package net.querz.mcaselector;

import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.text.Translation;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public final class Config {

	public static final File DEFAULT_BASE_DIR;

	static {
		// find jar file directory
		String jarPath;
		String path = ".";
		try {
			jarPath = Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			if (!jarPath.endsWith(".jar")) { // no jar
				path = jarPath.replaceAll("build/classes/java/main/$", "");
			} else {
				File jarFile = new File(jarPath);
				path = jarFile.getParent();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		DEFAULT_BASE_DIR = new File(path);
	}

	public static final File DEFAULT_BASE_CACHE_DIR = new File(DEFAULT_BASE_DIR, "cache");

	public static final Color DEFAULT_REGION_SELECTION_COLOR = new Color(1, 0.45, 0, 0.8);
	public static final Color DEFAULT_CHUNK_SELECTION_COLOR = new Color(1, 0.45, 0, 0.8);
	public static final Color DEFAULT_PASTE_CHUNKS_COLOR = new Color(0, 1, 0, 0.8);
	public static final Locale DEFAULT_LOCALE = Locale.UK;
	public static final int DEFAULT_LOAD_THREADS = 1;
	public static final int DEFAULT_PROCESS_THREADS = Math.max(Runtime.getRuntime().availableProcessors() - 2, 1);
	public static final int DEFAULT_WRITE_THREADS = Math.min(Runtime.getRuntime().availableProcessors(), 4);
	public static final int DEFAULT_MAX_LOADED_FILES = (int) Math.max(Math.ceil(Runtime.getRuntime().maxMemory() / 1_000_000_000D) * 2, 1);
	public static final boolean DEFAULT_SHADE = true;
	public static final boolean DEFAULT_SHADE_WATER = true;
	public static final boolean DEFAULT_DEBUG = false;

	private static File worldDir = null;
	private static UUID worldUUID = null;
	private static File baseCacheDir = DEFAULT_BASE_CACHE_DIR;
	private static File cacheDir = null;

	private static Locale locale = DEFAULT_LOCALE;
	private static Color regionSelectionColor = DEFAULT_REGION_SELECTION_COLOR;
	private static Color chunkSelectionColor = DEFAULT_CHUNK_SELECTION_COLOR;
	private static Color pasteChunksColor = DEFAULT_PASTE_CHUNKS_COLOR;
	private static int loadThreads = DEFAULT_LOAD_THREADS;
	private static int processThreads = DEFAULT_PROCESS_THREADS;
	private static int writeThreads = DEFAULT_WRITE_THREADS;
	private static int maxLoadedFiles = DEFAULT_MAX_LOADED_FILES;
	private static boolean shade = DEFAULT_SHADE;
	private static boolean shadeWater = DEFAULT_SHADE_WATER;

	private static boolean debug = DEFAULT_DEBUG;

	public static final float MAX_SCALE = 7.9999f;
	public static final float MIN_SCALE = 0.2f;
	public static final double IMAGE_POOL_SIZE = 2.5;

	private Config() {}

	public static File getWorldDir() {
		return worldDir;
	}

	public static void setWorldDir(File worldDir) {
		Config.worldDir = worldDir;
		worldUUID = UUID.nameUUIDFromBytes(worldDir.getAbsolutePath().getBytes());
		cacheDir = new File(baseCacheDir, worldUUID.toString().replace("-", ""));
	}

	public static File getCacheDirForWorldUUID(UUID world, int zoomLevel) {
		return new File(baseCacheDir, world.toString().replace("-", "") + "/" + zoomLevel);
	}

	public static UUID getWorldUUID() {
		return worldUUID;
	}

	public static File getCacheDir() {
		return cacheDir;
	}

	public static void setCacheDir(File cacheDir) {
		Config.cacheDir = cacheDir;
	}

	public static File[] getCacheDirs() {
		int lodLevels = 0;
		for (int i = getMaxZoomLevel(); i >= 1; i /= 2) {
			lodLevels++;
		}
		File[] cacheDirs = new File[lodLevels];
		for (int i = 0; i < cacheDirs.length; i++) {
			int zoomLevel = (int) Math.pow(2, i);
			cacheDirs[i] = new File(getCacheDir(), zoomLevel + "");
		}
		return cacheDirs;
	}

	public static void setShade(boolean shade) {
		Config.shade = shade;
	}

	public static boolean shade() {
		return Config.shade;
	}

	public static void setShadeWater(boolean shadeWater) {
		Config.shadeWater = shadeWater;
	}

	public static boolean shadeWater() {
		return Config.shadeWater;
	}

	public static void setDebug(boolean debug) {
		if (debug && !Config.debug) {
			Config.debug = true;
			Debug.initLogWriter();
		} else if (!debug && Config.debug) {
			Config.debug = false;
			Debug.flushAndCloseLogWriter();
		}
	}

	public static boolean debug() {
		return Config.debug;
	}

	public static Locale getLocale() {
		return locale;
	}

	public static void setLocale(Locale locale) {
		Config.locale = locale;
		Translation.load(locale);
	}

	public static Color getRegionSelectionColor() {
		return regionSelectionColor;
	}

	public static void setRegionSelectionColor(Color regionSelectionColor) {
		Config.regionSelectionColor = regionSelectionColor;
	}

	public static void loadFromIni() {
		File file = new File(DEFAULT_BASE_DIR, "settings.ini");
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
		} catch (IOException ex) {
			Debug.dumpException("failed to read settings.ini", ex);
		}

		try {
			//set values
			baseCacheDir = new File(config.getOrDefault(
				"BaseCacheDir",
				baseCacheDir.getAbsolutePath()).replace("{user.dir}", DEFAULT_BASE_CACHE_DIR.getAbsolutePath())
			);

			String localeString = config.getOrDefault("Locale", DEFAULT_LOCALE.toString());
			String[] localeSplit = localeString.split("_");
			setLocale(new Locale(localeSplit[0], localeSplit[1]));

			regionSelectionColor = new Color(config.getOrDefault("RegionSelectionColor", DEFAULT_REGION_SELECTION_COLOR.toString()));
			chunkSelectionColor = new Color(config.getOrDefault("ChunkSelectionColor", DEFAULT_CHUNK_SELECTION_COLOR.toString()));
			pasteChunksColor = new Color(config.getOrDefault("PasteChunksColor", DEFAULT_PASTE_CHUNKS_COLOR.toString()));
			loadThreads = Integer.parseInt(config.getOrDefault("LoadThreads", DEFAULT_LOAD_THREADS + ""));
			processThreads = Integer.parseInt(config.getOrDefault("ProcessThreads", DEFAULT_PROCESS_THREADS + ""));
			writeThreads = Integer.parseInt(config.getOrDefault("WriteThreads", DEFAULT_WRITE_THREADS + ""));
			maxLoadedFiles = Integer.parseInt(config.getOrDefault("MaxLoadedFiles", DEFAULT_MAX_LOADED_FILES + ""));
			shade = Boolean.parseBoolean(config.getOrDefault("Shade", DEFAULT_SHADE + ""));
			shadeWater = Boolean.parseBoolean(config.getOrDefault("ShadeWater", DEFAULT_SHADE_WATER + ""));
			debug = Boolean.parseBoolean(config.getOrDefault("Debug", DEFAULT_DEBUG + ""));
		} catch (Exception ex) {
			Debug.dumpException("error loading settings.ini", ex);
		}
	}

	public static void exportConfig() {
		String userDir = DEFAULT_BASE_DIR.getAbsolutePath();
		File file = new File(userDir, "settings.ini");
		List<String> lines = new ArrayList<>(8);
		addSettingsLine(
			"BaseCacheDir",
			baseCacheDir.getAbsolutePath().replace(userDir, "{user.dir}"),
			DEFAULT_BASE_CACHE_DIR.getAbsolutePath().replace(userDir, "{user.dir}"), lines);
		addSettingsLine("Locale", locale.toString(), DEFAULT_LOCALE.toString(), lines);
		addSettingsLine("RegionSelectionColor", regionSelectionColor.toString(), DEFAULT_REGION_SELECTION_COLOR.toString(), lines);
		addSettingsLine("ChunkSelectionColor", chunkSelectionColor.toString(), DEFAULT_CHUNK_SELECTION_COLOR.toString(), lines);
		addSettingsLine("PasteChunksColor", pasteChunksColor.toString(), DEFAULT_PASTE_CHUNKS_COLOR.toString(), lines);
		addSettingsLine("LoadThreads", loadThreads, DEFAULT_LOAD_THREADS, lines);
		addSettingsLine("ProcessThreads", processThreads, DEFAULT_PROCESS_THREADS, lines);
		addSettingsLine("WriteThreads", writeThreads, DEFAULT_WRITE_THREADS, lines);
		addSettingsLine("MaxLoadedFiles", maxLoadedFiles, DEFAULT_MAX_LOADED_FILES, lines);
		addSettingsLine("Shade", shade, DEFAULT_SHADE, lines);
		addSettingsLine("ShadeWater", shadeWater, DEFAULT_SHADE_WATER, lines);
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
			Debug.dumpException("error writing settings.ini", ex);
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

	public static Color getPasteChunksColor() {
		return pasteChunksColor;
	}

	public static void setPasteChunksColor(Color pasteChunksColor) {
		Config.pasteChunksColor = pasteChunksColor;
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

	public static int getMaxZoomLevel() {
		return Tile.getZoomLevel(MAX_SCALE);
	}

	public static int getMinZoomLevel() {
		return Tile.getZoomLevel(MIN_SCALE);
	}
}
