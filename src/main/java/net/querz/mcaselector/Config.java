package net.querz.mcaselector;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.text.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

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
	public static final File DEFAULT_BASE_LOG_DIR;
	public static final File DEFAULT_BASE_CONFIG_FILE;
	public static final File DEFAULT_BASE_OVERLAYS_FILE;

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("mac")) {
			DEFAULT_BASE_CACHE_DIR = new File(System.getProperty("user.home"), "Library/Caches/mcaselector");
			DEFAULT_BASE_LOG_DIR = new File(System.getProperty("user.home"), "Library/Logs/mcaselector");
			DEFAULT_BASE_CONFIG_FILE = new File(System.getProperty("user.home"), "Library/Application Support/mcaselector/settings.ini");
			DEFAULT_BASE_OVERLAYS_FILE = new File(System.getProperty("user.home"), "Library/Application Support/mcaselector/overlays.json");
		} else if (osName.contains("windows")) {
			DEFAULT_BASE_CACHE_DIR = getEnvFilesWithDefault(DEFAULT_BASE_DIR.getAbsolutePath(), "mcaselector/cache", ';', "LOCALAPPDATA");
			DEFAULT_BASE_LOG_DIR = getEnvFilesWithDefault(DEFAULT_BASE_DIR.getAbsolutePath(), "mcaselector/log", ';', "LOCALAPPDATA");
			DEFAULT_BASE_CONFIG_FILE = getEnvFilesWithDefault(DEFAULT_BASE_DIR.getAbsolutePath(), "mcaselector/settings.ini", ';', "LOCALAPPDATA");
			DEFAULT_BASE_OVERLAYS_FILE = getEnvFilesWithDefault(DEFAULT_BASE_DIR.getAbsolutePath(), "mcaselector/overlays.json", ';', "LOCALAPPDATA");
		} else {
			DEFAULT_BASE_CACHE_DIR = getEnvFilesWithDefault("~/.cache", "mcaselector", ':', "XDG_CACHE_HOME", "XDG_CACHE_DIRS");
			DEFAULT_BASE_LOG_DIR = getEnvFilesWithDefault("~/.local/share", "mcaselector/log", ':', "XDG_DATA_HOME", "XDG_DATA_DIRS");
			DEFAULT_BASE_CONFIG_FILE = getEnvFilesWithDefault("~/.mcaselector", "mcaselector/settings.ini", ':', "XDG_CONFIG_HOME", "XDG_CONFIG_DIRS");
			DEFAULT_BASE_OVERLAYS_FILE = getEnvFilesWithDefault("~/.mcaselector", "mcaselector/overlays.json", ':', "XDG_CONFIG_HOME", "XDG_CONFIG_DIRS");
		}

		if (!DEFAULT_BASE_CACHE_DIR.exists()) {
			DEFAULT_BASE_CACHE_DIR.mkdirs();
		}
		if (!DEFAULT_BASE_LOG_DIR.exists()) {
			DEFAULT_BASE_LOG_DIR.mkdirs();
		}
		if (!DEFAULT_BASE_OVERLAYS_FILE.getParentFile().exists()) {
			DEFAULT_BASE_OVERLAYS_FILE.getParentFile().mkdirs();
		}
		Logging.setLogDir(DEFAULT_BASE_LOG_DIR);
	}

	private static File getEnvFilesWithDefault(String def, String suffix, char divider, String... envs) {
		File file;
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
					file = new File(resolveHome(split[0]), suffix);
				} else {
					file = new File(resolveHome(value), suffix);
				}
				if (attemptCreateDirectory(file)) {
					return file;
				}
			}
		}
		file = new File(resolveHome(def), suffix);
		if (attemptCreateDirectory(file)) {
			return file;
		}
		throw new RuntimeException("failed to create directories for " + suffix + ", please check permissions for " + resolveHome(def));
	}

	private static boolean attemptCreateDirectory(File file) {
		File parent = file.getParentFile();
		if (parent == null) {
			return false;
		}
		try {
			Files.createDirectories(parent.getCanonicalFile().toPath());
			return true;
		} catch (IOException ex) {
//			LOGGER.warn("failed to create directory {}", parent, ex);
			return false;
		}
	}

	private static String resolveHome(String dir) {
		if (dir.startsWith("~/")) {
			return System.getProperty("user.home") + dir.substring(1);
		}
		return dir;
	}

	private static final Logger LOGGER;

	static {
		LOGGER = LogManager.getLogger(Config.class);
	}

	public static final Color DEFAULT_REGION_SELECTION_COLOR = new Color(1, 0.45, 0, 0.8);
	public static final Color DEFAULT_CHUNK_SELECTION_COLOR = new Color(1, 0.45, 0, 0.8);
	public static final Color DEFAULT_PASTE_CHUNKS_COLOR = new Color(0, 1, 0, 0.8);
	public static final Locale DEFAULT_LOCALE = Locale.UK;
	public static final int DEFAULT_PROCESS_THREADS = Math.min(Math.max(Runtime.getRuntime().availableProcessors() - 2, 1), 4);
	public static final int DEFAULT_WRITE_THREADS = Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 1), 4);
	public static final int DEFAULT_MAX_LOADED_FILES = (int) Math.min(Math.max(Math.ceil(Runtime.getRuntime().maxMemory() / 1_000_000_000D) * 2, 1), 16);
	public static final boolean DEFAULT_SHADE = true;
	public static final boolean DEFAULT_SHADE_WATER = true;
	public static final boolean DEFAULT_SHOW_NONEXISTENT_REGIONS = true;
	public static final boolean DEFAULT_SMOOTH_RENDERING = false;
	public static final boolean DEFAULT_SMOOTH_OVERLAYS = true;
	public static final String DEFAULT_TILEMAP_BACKGROUND = "BLACK";
	public static final boolean DEFAULT_DEBUG = false;
	public static final String DEFAULT_MC_SAVES_DIR = FileHelper.getMCSavesDir();

	public static final int DEFAULT_RENDER_HEIGHT = 319;
	public static final boolean DEFAULT_RENDER_LAYER_ONLY = false;
	public static final boolean DEFAULT_RENDER_CAVES = false;

	private static File worldDir = null;
	private static WorldDirectories worldDirs = null;
	private static UUID worldUUID = null;
	private static File baseCacheDir = DEFAULT_BASE_CACHE_DIR;
	private static File logDir = DEFAULT_BASE_LOG_DIR;
	private static File cacheDir = null;

	private static Locale locale = DEFAULT_LOCALE;
	private static Color regionSelectionColor = DEFAULT_REGION_SELECTION_COLOR;
	private static Color chunkSelectionColor = DEFAULT_CHUNK_SELECTION_COLOR;
	private static Color pasteChunksColor = DEFAULT_PASTE_CHUNKS_COLOR;
	private static int processThreads = DEFAULT_PROCESS_THREADS;
	private static int writeThreads = DEFAULT_WRITE_THREADS;
	private static int maxLoadedFiles = DEFAULT_MAX_LOADED_FILES;
	private static boolean shade = DEFAULT_SHADE;
	private static boolean shadeWater = DEFAULT_SHADE_WATER;
	private static boolean showNonexistentRegions = DEFAULT_SHOW_NONEXISTENT_REGIONS;
	private static boolean smoothRendering = DEFAULT_SMOOTH_RENDERING;
	private static boolean smoothOverlays = DEFAULT_SMOOTH_OVERLAYS;
	private static String tileMapBackground = DEFAULT_TILEMAP_BACKGROUND;
	private static String mcSavesDir = DEFAULT_MC_SAVES_DIR;

	private static int renderHeight = DEFAULT_RENDER_HEIGHT;
	private static boolean renderLayerOnly = DEFAULT_RENDER_LAYER_ONLY;
	private static boolean renderCaves = DEFAULT_RENDER_CAVES;

	private static boolean debug = DEFAULT_DEBUG;

	public static final float MAX_SCALE = 15.9999f;
	public static final float MIN_SCALE = 0.05f;
	public static final double IMAGE_POOL_SIZE = 2.5;

	private static List<Overlay> overlays = null;

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
		LOGGER.debug("setting world directories to {}", dirs);
		Config.worldDirs = dirs;
		Config.worldDir = dirs.getRegion();
		worldUUID = UUID.nameUUIDFromBytes(worldDir.getAbsolutePath().getBytes());
		cacheDir = new File(baseCacheDir, worldUUID.toString().replace("-", ""));
	}

	public static File getCacheDirForWorldUUID(UUID world, int zoomLevel) {
		if (world == null) {
			return new File(cacheDir, "" + zoomLevel);
		}
		return new File(cacheDir, world.toString().replace("-", "") + "/" + zoomLevel);
	}

	public static File getCacheDirForWorldUUID(UUID world) {
		if (world == null) {
			return cacheDir;
		}
		return new File(cacheDir, world.toString().replace("-", ""));
	}

	public static UUID getWorldUUID() {
		return worldUUID;
	}

	public static File getCacheDir() {
		return cacheDir;
	}

	public static void setCacheDir(File cacheDir) {
		Config.cacheDir = new File(cacheDir, worldUUID.toString().replace("-", ""));;
	}

	public static File getCacheDir(int zoomLevel) {
		return new File(cacheDir, "" + zoomLevel);
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

	public static File getLogDir() {
		return logDir;
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

	public static void setShowNonExistentRegions(boolean showNonexistentRegions) {
		Config.showNonexistentRegions = showNonexistentRegions;
	}

	public static boolean showNonExistentRegions() {
		return Config.showNonexistentRegions;
	}

	public static void setSmoothRendering(boolean smoothRendering) {
		Config.smoothRendering = smoothRendering;
	}

	public static boolean smoothRendering() {
		return Config.smoothRendering;
	}

	public static void setSmoothOverlays(boolean smoothOverlays) {
		Config.smoothOverlays = smoothOverlays;
	}

	public static boolean smoothOverlays() {
		return Config.smoothOverlays;
	}

	public static void setTileMapBackground(String tileMapBackground) {
		Config.tileMapBackground = tileMapBackground;
	}

	public static String getTileMapBackground() {
		return Config.tileMapBackground;
	}

	public static void setRenderHeight(int renderHeight) {
		Config.renderHeight = renderHeight;
	}

	public static int getRenderHeight() {
		return Config.renderHeight;
	}

	public static void setRenderLayerOnly(boolean renderLayerOnly) {
		Config.renderLayerOnly = renderLayerOnly;
	}

	public static boolean renderLayerOnly() {
		return Config.renderLayerOnly;
	}

	public static void setRenderCaves(boolean renderCaves) {
		Config.renderCaves = renderCaves;
	}

	public static boolean renderCaves() {
		return Config.renderCaves;
	}

	public static void setMCSavesDir(String mcSavesDir) {
		Config.mcSavesDir = mcSavesDir;
	}

	public static String getMCSavesDir() {
		return Config.mcSavesDir;
	}

	public static void setDebug(boolean debug) {
		Config.debug = debug;
		if (debug) {
			Logging.setLogLevel(Logging.DEBUG);
		} else {
			Logging.setLogLevel(Logging.WARN);
		}
		Logging.updateThreadContext();
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

	public static void setOverlays(List<Overlay> overlays) {
		Config.overlays = overlays;
	}

	public static List<Overlay> getOverlays() {
		return Config.overlays;
	}

	public static Color getRegionSelectionColor() {
		return regionSelectionColor;
	}

	public static void setRegionSelectionColor(Color regionSelectionColor) {
		Config.regionSelectionColor = regionSelectionColor;
	}

	public static void loadFromIni() {
		if (DEFAULT_BASE_CONFIG_FILE.exists()) {
			String userDir = DEFAULT_BASE_DIR.getAbsolutePath();
			Map<String, String> config = new HashMap<>();
			try (Stream<String> lines = Files.lines(DEFAULT_BASE_CONFIG_FILE.toPath())) {
				lines.forEach(l -> {
					if (l.charAt(0) == ';') {
						return;
					}
					String[] elements = l.split("=", 2);
					if (elements.length != 2) {
						LOGGER.error("invalid line in settings: \"{}\"", l);
						return;
					}
					config.put(elements[0], elements[1]);
				});
			} catch (IOException ex) {
				LOGGER.warn("failed to read settings", ex);
			}

			try {
				//set values
				baseCacheDir = new File(config.getOrDefault(
						"BaseCacheDir",
						DEFAULT_BASE_CACHE_DIR.getAbsolutePath()).replace("{user.dir}", userDir)
				);
				logDir = new File(config.getOrDefault(
						"LogDir",
						DEFAULT_BASE_LOG_DIR.getAbsolutePath()).replace("{user.dir}", userDir)
				);

				String localeString = config.getOrDefault("Locale", DEFAULT_LOCALE.toString());
				String[] localeSplit = localeString.split("_");
				setLocale(new Locale(localeSplit[0], localeSplit[1]));

				regionSelectionColor = new Color(config.getOrDefault("RegionSelectionColor", DEFAULT_REGION_SELECTION_COLOR.toString()));
				chunkSelectionColor = new Color(config.getOrDefault("ChunkSelectionColor", DEFAULT_CHUNK_SELECTION_COLOR.toString()));
				pasteChunksColor = new Color(config.getOrDefault("PasteChunksColor", DEFAULT_PASTE_CHUNKS_COLOR.toString()));
				processThreads = Integer.parseInt(config.getOrDefault("ProcessThreads", DEFAULT_PROCESS_THREADS + ""));
				writeThreads = Integer.parseInt(config.getOrDefault("WriteThreads", DEFAULT_WRITE_THREADS + ""));
				maxLoadedFiles = Integer.parseInt(config.getOrDefault("MaxLoadedFiles", DEFAULT_MAX_LOADED_FILES + ""));
				mcSavesDir = config.getOrDefault("MCSavesDir", DEFAULT_MC_SAVES_DIR);
				if (!new File(mcSavesDir).exists()) {
					mcSavesDir = DEFAULT_MC_SAVES_DIR;
				}
				setDebug(Boolean.parseBoolean(config.getOrDefault("Debug", DEFAULT_DEBUG + "")));
			} catch (Exception ex) {
				LOGGER.warn("error loading settings", ex);
			}
		}


		// load overlays
		if (DEFAULT_BASE_OVERLAYS_FILE.exists()) {
			JSONArray overlayArray = null;
			try {
				overlayArray = new JSONArray(new String(Files.readAllBytes(DEFAULT_BASE_OVERLAYS_FILE.toPath())));
			} catch (IOException ex) {
				LOGGER.warn("failed to read overlays", ex);
			}
			if (overlayArray != null) {
				List<Overlay> overlays = new ArrayList<>();
				for (Object o : overlayArray) {
					try {
						overlays.add(Overlay.fromJSON((JSONObject) o));
					} catch (Exception ex) {
						LOGGER.warn("failed to parse overlay", ex);
					}
				}
				Config.overlays = overlays;
			}
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
				"LogDir",
				logDir.getAbsolutePath().startsWith(userDir) ? logDir.getAbsolutePath().replace(userDir, "{user.dir}") : logDir.getAbsolutePath(),
				DEFAULT_BASE_LOG_DIR.getAbsolutePath().replace(userDir, "{user.dir}"), lines);
		addSettingsLine("Locale", locale.toString(), DEFAULT_LOCALE.toString(), lines);
		addSettingsLine("RegionSelectionColor", regionSelectionColor.toString(), DEFAULT_REGION_SELECTION_COLOR.toString(), lines);
		addSettingsLine("ChunkSelectionColor", chunkSelectionColor.toString(), DEFAULT_CHUNK_SELECTION_COLOR.toString(), lines);
		addSettingsLine("PasteChunksColor", pasteChunksColor.toString(), DEFAULT_PASTE_CHUNKS_COLOR.toString(), lines);
		addSettingsLine("ProcessThreads", processThreads, DEFAULT_PROCESS_THREADS, lines);
		addSettingsLine("WriteThreads", writeThreads, DEFAULT_WRITE_THREADS, lines);
		addSettingsLine("MaxLoadedFiles", maxLoadedFiles, DEFAULT_MAX_LOADED_FILES, lines);
		addSettingsLine("MCSavesDir", mcSavesDir, DEFAULT_MC_SAVES_DIR, lines);
		addSettingsLine("Debug", debug, DEFAULT_DEBUG, lines);
		if (lines.size() == 0) {
			if (DEFAULT_BASE_CONFIG_FILE.exists() && !DEFAULT_BASE_CONFIG_FILE.delete()) {
				LOGGER.warn("failed to delete {}", DEFAULT_BASE_CONFIG_FILE.getAbsolutePath());
			}
			return;
		}
		try {
			Files.write(DEFAULT_BASE_CONFIG_FILE.toPath(), lines);
		} catch (IOException ex) {
			LOGGER.warn("error writing settings", ex);
		}

		// save overlays
		if (overlays == null) {
			if (DEFAULT_BASE_OVERLAYS_FILE.exists() && !DEFAULT_BASE_OVERLAYS_FILE.delete()) {
				LOGGER.warn("failed to delete {}", DEFAULT_BASE_OVERLAYS_FILE.getAbsolutePath());
			}
		} else {
			JSONArray overlayArray = new JSONArray();
			for (Overlay parser : overlays) {
				overlayArray.put(parser.toJSON());
			}
			try {
				Files.write(DEFAULT_BASE_OVERLAYS_FILE.toPath(), Collections.singleton(overlayArray.toString()));
			} catch (IOException ex) {
				LOGGER.warn("error writing overlays", ex);
			}
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

	public static String asString() {
		final StringBuilder sb = new StringBuilder("Config{\n");
		sb.append(" DEFAULT_BASE_DIR=").append(DEFAULT_BASE_DIR);
		sb.append(",\n DEFAULT_BASE_CACHE_DIR=").append(DEFAULT_BASE_CACHE_DIR);
		sb.append(",\n DEFAULT_BASE_LOG_DIR=").append(DEFAULT_BASE_LOG_DIR);
		sb.append(",\n DEFAULT_BASE_CONFIG_FILE=").append(DEFAULT_BASE_CONFIG_FILE);
		sb.append(",\n DEFAULT_BASE_OVERLAYS_FILE=").append(DEFAULT_BASE_OVERLAYS_FILE);
		sb.append(",\n DEFAULT_REGION_SELECTION_COLOR=").append(DEFAULT_REGION_SELECTION_COLOR);
		sb.append(",\n DEFAULT_CHUNK_SELECTION_COLOR=").append(DEFAULT_CHUNK_SELECTION_COLOR);
		sb.append(",\n DEFAULT_PASTE_CHUNKS_COLOR=").append(DEFAULT_PASTE_CHUNKS_COLOR);
		sb.append(",\n DEFAULT_LOCALE=").append(DEFAULT_LOCALE);
		sb.append(",\n DEFAULT_PROCESS_THREADS=").append(DEFAULT_PROCESS_THREADS);
		sb.append(",\n DEFAULT_WRITE_THREADS=").append(DEFAULT_WRITE_THREADS);
		sb.append(",\n DEFAULT_MAX_LOADED_FILES=").append(DEFAULT_MAX_LOADED_FILES);
		sb.append(",\n DEFAULT_SHADE=").append(DEFAULT_SHADE);
		sb.append(",\n DEFAULT_SHADE_WATER=").append(DEFAULT_SHADE_WATER);
		sb.append(",\n DEFAULT_SHOW_NONEXISTENT_REGIONS=").append(DEFAULT_SHOW_NONEXISTENT_REGIONS);
		sb.append(",\n DEFAULT_SMOOTH_RENDERING=").append(DEFAULT_SMOOTH_RENDERING);
		sb.append(",\n DEFAULT_SMOOTH_OVERLAYS=").append(DEFAULT_SMOOTH_OVERLAYS);
		sb.append(",\n DEFAULT_TILEMAP_BACKGROUND='").append(DEFAULT_TILEMAP_BACKGROUND).append('\'');
		sb.append(",\n DEFAULT_DEBUG=").append(DEFAULT_DEBUG);
		sb.append(",\n DEFAULT_MC_SAVES_DIR='").append(DEFAULT_MC_SAVES_DIR).append('\'');
		sb.append(",\n DEFAULT_RENDER_HEIGHT=").append(DEFAULT_RENDER_HEIGHT);
		sb.append(",\n DEFAULT_RENDER_LAYER_ONLY=").append(DEFAULT_RENDER_LAYER_ONLY);
		sb.append(",\n DEFAULT_RENDER_CAVES=").append(DEFAULT_RENDER_CAVES);
		sb.append(",\n worldDir=").append(worldDir);
		sb.append(",\n worldDirs=").append(worldDirs);
		sb.append(",\n worldUUID=").append(worldUUID);
		sb.append(",\n baseCacheDir=").append(baseCacheDir);
		sb.append(",\n logDir=").append(logDir);
		sb.append(",\n cacheDir=").append(cacheDir);
		sb.append(",\n locale=").append(locale);
		sb.append(",\n regionSelectionColor=").append(regionSelectionColor);
		sb.append(",\n chunkSelectionColor=").append(chunkSelectionColor);
		sb.append(",\n pasteChunksColor=").append(pasteChunksColor);
		sb.append(",\n processThreads=").append(processThreads);
		sb.append(",\n writeThreads=").append(writeThreads);
		sb.append(",\n maxLoadedFiles=").append(maxLoadedFiles);
		sb.append(",\n shade=").append(shade);
		sb.append(",\n shadeWater=").append(shadeWater);
		sb.append(",\n showNonexistentRegions=").append(showNonexistentRegions);
		sb.append(",\n smoothRendering=").append(smoothRendering);
		sb.append(",\n smoothOverlays=").append(smoothOverlays);
		sb.append(",\n tileMapBackground='").append(tileMapBackground).append('\'');
		sb.append(",\n mcSavesDir='").append(mcSavesDir).append('\'');
		sb.append(",\n renderHeight=").append(renderHeight);
		sb.append(",\n renderLayerOnly=").append(renderLayerOnly);
		sb.append(",\n renderCaves=").append(renderCaves);
		sb.append(",\n debug=").append(debug);
		sb.append(",\n MAX_SCALE=").append(MAX_SCALE);
		sb.append(",\n MIN_SCALE=").append(MIN_SCALE);
		sb.append(",\n IMAGE_POOL_SIZE=").append(IMAGE_POOL_SIZE);
		sb.append("\n}");
		return sb.toString();
	}
}
