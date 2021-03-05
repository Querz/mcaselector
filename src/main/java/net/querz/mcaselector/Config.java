package net.querz.mcaselector;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.WorldDirectories;
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

	public static final File DEFAULT_BASE_CACHE_DIR;
	public static final File DEFAULT_BASE_LOG_FILE;
	public static final File DEFAULT_BASE_CONFIG_FILE;

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("mac")) {
			DEFAULT_BASE_CACHE_DIR = new File(System.getProperty("user.home"), "Library/Caches/mcaselector");
			DEFAULT_BASE_LOG_FILE = new File(System.getProperty("user.home"), "Library/Logs/mcaselector/debug.log");
			DEFAULT_BASE_CONFIG_FILE = new File(System.getProperty("user.home"), "Library/Application Support/mcaselector/settings.ini");
		} else if (osName.contains("windows")) {
			DEFAULT_BASE_CACHE_DIR = getEnvFilesWithDefault(DEFAULT_BASE_DIR.getAbsolutePath(), "mcaselector/cache", ';', "LOCALAPPDATA");
			DEFAULT_BASE_LOG_FILE = getEnvFilesWithDefault(DEFAULT_BASE_DIR.getAbsolutePath(), "mcaselector/debug.log", ';', "LOCALAPPDATA");
			DEFAULT_BASE_CONFIG_FILE = getEnvFilesWithDefault(DEFAULT_BASE_DIR.getAbsolutePath(), "mcaselector/settings.ini", ';', "LOCALAPPDATA");
		} else {
			DEFAULT_BASE_CACHE_DIR = getEnvFilesWithDefault("~/.cache", "mcaselector", ':', "XDG_CACHE_HOME", "XDG_CACHE_DIRS");
			DEFAULT_BASE_LOG_FILE = getEnvFilesWithDefault("~/.local/share", "mcaselector/debug.log", ';', "XDG_DATA_HOME", "XDG_DATA_DIRS");
			DEFAULT_BASE_CONFIG_FILE = getEnvFilesWithDefault("~/.mcaselector", "mcaselector/settings.ini", ';', "XDG_CONFIG_HOME", "XDG_CONFIG_DIRS");
		}

		if (!DEFAULT_BASE_CACHE_DIR.exists()) {
			DEFAULT_BASE_CACHE_DIR.mkdirs();
		}
		if (!DEFAULT_BASE_LOG_FILE.getParentFile().exists()) {
			DEFAULT_BASE_LOG_FILE.getParentFile().mkdirs();
		}
		if (!DEFAULT_BASE_CONFIG_FILE.getParentFile().exists()) {
			DEFAULT_BASE_CONFIG_FILE.getParentFile().mkdirs();
		}
	}

	private static File getEnvFilesWithDefault(String def, String suffix, char divider, String... envs) {
		for (String env : envs) {
			String value = System.getenv(env);
			if (value != null && !value.isEmpty()) {
				String[] split = value.split("" + divider);
				if (split.length > 1) {
					for (String part : split) {
						File f = new File(resolveHome(part), suffix);
						if (f.exists()) {
							return f;
						}
					}
					return new File(resolveHome(split[0]), suffix);
				} else {
					return new File(resolveHome(value), suffix);
				}
			}
		}
		return new File(resolveHome(def), suffix);
	}

	private static String resolveHome(String dir) {
		if (dir.startsWith("~/")) {
			return System.getProperty("user.home") + dir.substring(1);
		}
		return dir;
	}

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
	public static final String DEFAULT_MC_SAVES_DIR = FileHelper.getMCSavesDir();

	private static File worldDir = null;
	private static WorldDirectories worldDirs = null;
	private static UUID worldUUID = null;
	private static File baseCacheDir = DEFAULT_BASE_CACHE_DIR;
	private static File logFile = DEFAULT_BASE_LOG_FILE;
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
	private static String mcSavesDir = DEFAULT_MC_SAVES_DIR;

	private static boolean debug = DEFAULT_DEBUG;

	public static final float MAX_SCALE = 15.9999f;
	public static final float MIN_SCALE = 0.2f;
	public static final double IMAGE_POOL_SIZE = 2.5;

	private Config() {}

	public static File getWorldDir() {
		return worldDir;
	}

	public static WorldDirectories getWorldDirs() {
		return worldDirs;
	}

	public static void setWorldDir(File worldDir) {
		Config.worldDir = worldDir;
		Config.worldDirs = new WorldDirectories(worldDir, null, null);
		worldUUID = UUID.nameUUIDFromBytes(worldDir.getAbsolutePath().getBytes());
		cacheDir = new File(baseCacheDir, worldUUID.toString().replace("-", ""));
	}

	public static void setWorldDirs(File regionDir, File poiDir, File entitiesDir) {
		Config.worldDirs = new WorldDirectories(regionDir, poiDir, entitiesDir);
		Config.worldDir = regionDir;
		worldUUID = UUID.nameUUIDFromBytes(worldDir.getAbsolutePath().getBytes());
		cacheDir = new File(baseCacheDir, worldUUID.toString().replace("-", ""));
	}

	public static void setWorldDirs(WorldDirectories dirs) {
		Debug.dumpf("setting world directories to %s", dirs);
		Config.worldDirs = dirs;
		Config.worldDir = dirs.getRegion();
		worldUUID = UUID.nameUUIDFromBytes(worldDir.getAbsolutePath().getBytes());
		cacheDir = new File(baseCacheDir, worldUUID.toString().replace("-", ""));
	}

	public static File getCacheDirForWorldUUID(UUID world, int zoomLevel) {
		return new File(baseCacheDir, world.toString().replace("-", "") + "/" + zoomLevel);
	}

	public static File getCacheDirForWorldUUID(UUID world) {
		return new File(baseCacheDir, world.toString().replace("-", ""));
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

	public static File getBaseCacheDir() {
		return baseCacheDir;
	}

	public static File getLogFile() {
		return logFile;
	}

	public static File getConfigFile() {
		return DEFAULT_BASE_CONFIG_FILE;
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

	public static void setMCSavesDir(String mcSavesDir) {
		Config.mcSavesDir = mcSavesDir;
	}

	public static String getMCSavesDir() {
		return Config.mcSavesDir;
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
		if (!DEFAULT_BASE_CONFIG_FILE.exists()) {
			return;
		}
		String userDir = DEFAULT_BASE_DIR.getAbsolutePath();
		Map<String, String> config = new HashMap<>();
		try {
			Files.lines(DEFAULT_BASE_CONFIG_FILE.toPath()).forEach(l -> {
				if (l.charAt(0) == ';') {
					return;
				}
				String[] elements = l.split("=", 2);
				if (elements.length != 2) {
					Debug.errorf("invalid line in settings: \"%s\"", l);
					return;
				}
				config.put(elements[0], elements[1]);
			});
		} catch (IOException ex) {
			Debug.dumpException("failed to read settings", ex);
		}

		try {
			//set values
			baseCacheDir = new File(config.getOrDefault(
					"BaseCacheDir",
					DEFAULT_BASE_CACHE_DIR.getAbsolutePath()).replace("{user.dir}", userDir)
			);
			logFile = new File(config.getOrDefault(
					"LogFile",
					DEFAULT_BASE_LOG_FILE.getAbsolutePath()).replace("{user.dir}", userDir)
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
			mcSavesDir = config.getOrDefault("MCSavesDir", DEFAULT_MC_SAVES_DIR);
			if (!new File(mcSavesDir).exists()) {
				mcSavesDir = DEFAULT_MC_SAVES_DIR;
			}
			debug = Boolean.parseBoolean(config.getOrDefault("Debug", DEFAULT_DEBUG + ""));
		} catch (Exception ex) {
			Debug.dumpException("error loading settings", ex);
		}
	}

	public static void exportConfig() {
		String userDir = DEFAULT_BASE_DIR.getAbsolutePath();
		List<String> lines = new ArrayList<>(8);
		addSettingsLine(
				"BaseCacheDir",
				baseCacheDir.getAbsolutePath().startsWith(userDir) ? baseCacheDir.getAbsolutePath().replace(userDir, "{user.dir}") : baseCacheDir.getAbsolutePath(),
				DEFAULT_BASE_CACHE_DIR.getAbsolutePath().replace(userDir, "{user.dir}"), lines);
		addSettingsLine(
				"LogFile",
				logFile.getAbsolutePath().startsWith(userDir) ? logFile.getAbsolutePath().replace(userDir, "{user.dir}") : logFile.getAbsolutePath(),
				DEFAULT_BASE_LOG_FILE.getAbsolutePath().replace(userDir, "{user.dir}"), lines);
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
		addSettingsLine("MCSavesDir", mcSavesDir, DEFAULT_MC_SAVES_DIR, lines);
		addSettingsLine("Debug", debug, DEFAULT_DEBUG, lines);
		if (lines.size() == 0) {
			if (DEFAULT_BASE_CONFIG_FILE.exists() && !DEFAULT_BASE_CONFIG_FILE.delete()) {
				Debug.errorf("could not delete %s", DEFAULT_BASE_CONFIG_FILE.getAbsolutePath());
			}
			return;
		}
		try {
			Files.write(DEFAULT_BASE_CONFIG_FILE.toPath(), lines);
		} catch (IOException ex) {
			Debug.dumpException("error writing settings", ex);
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
