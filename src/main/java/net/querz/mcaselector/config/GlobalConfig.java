package net.querz.mcaselector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.*;
import net.querz.mcaselector.config.adapter.ColorAdapter;
import net.querz.mcaselector.config.adapter.FileAdapter;
import net.querz.mcaselector.config.adapter.LocaleAdapter;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.logging.GsonNamingStrategy;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.util.property.MSWindowsRegistry;
import net.querz.mcaselector.version.mapping.registry.StructureRegistry;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

public class GlobalConfig extends Config {

	private static final Gson gsonInstance;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Color.class, new ColorAdapter());
		builder.registerTypeAdapter(File.class, new FileAdapter(BASE_DIR.getAbsolutePath()));
		builder.registerTypeAdapter(Locale.class, new LocaleAdapter());
		builder.setPrettyPrinting();
		gsonInstance = builder.create();
	}

	// defaults
	public static final Color DEFAULT_REGION_SELECTION_COLOR = new Color(1, 0.45, 0, 0.8);
	public static final Color DEFAULT_CHUNK_SELECTION_COLOR = new Color(1, 0.45, 0, 0.8);
	public static final Color DEFAULT_PASTE_CHUNKS_COLOR = new Color(0, 1, 0, 0.8);
	public static final Locale DEFAULT_LOCALE = Locale.UK;
	public static final int DEFAULT_PROCESS_THREADS = Math.min(Math.max(Runtime.getRuntime().availableProcessors() - 2, 1), 4);
	public static final int DEFAULT_WRITE_THREADS = Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 1), 4);
	public static final int DEFAULT_MAX_LOADED_FILES = 1;
	public static final boolean DEFAULT_DEBUG = false;
	public static final String DEFAULT_MC_SAVES_DIR = FileHelper.getMCSavesDir();
	public static final boolean DEFAULT_SHOW_CHUNK_GRID = true;
	public static final boolean DEFAULT_SHOW_REGION_GRID = true;
	public static final boolean DEFAULT_SHOW_COORDINATES = false;
	public static final String DEFAULT_COORDINATE_STYLE = "DYNAMIC";

	// attributes
	private Locale locale = DEFAULT_LOCALE;
	private Color regionSelectionColor = DEFAULT_REGION_SELECTION_COLOR;
	private Color chunkSelectionColor = DEFAULT_CHUNK_SELECTION_COLOR;
	private Color pasteChunksColor = DEFAULT_PASTE_CHUNKS_COLOR;
	private int processThreads = DEFAULT_PROCESS_THREADS;
	private int writeThreads = DEFAULT_WRITE_THREADS;
	private int maxLoadedFiles = DEFAULT_MAX_LOADED_FILES;
	private String mcSavesDir = DEFAULT_MC_SAVES_DIR;
	private boolean showChunkGrid = DEFAULT_SHOW_CHUNK_GRID;
	private boolean showRegionGrid = DEFAULT_SHOW_REGION_GRID;
	private boolean showCoordinates = DEFAULT_SHOW_COORDINATES;
	private String coordinateStyle = DEFAULT_COORDINATE_STYLE;
	private boolean debug = DEFAULT_DEBUG;
	private final TreeMap<Long, RecentWorld> recentWorlds = new TreeMap<>();
	private final RecentFiles recentFilterScripts = new RecentFiles();
	private final RecentFiles recentChangeScripts = new RecentFiles();
	private final RecentFiles recentOverlayScripts = new RecentFiles();
	private final List<ReplaceBlocksUserPreset> replaceBlocksUserPresets = new ArrayList<>();
	private TempScript filterScript = new TempScript(null, false, "");
	private TempScript changeScript = new TempScript(null, false, "");
	private TempScript overlayScript = new TempScript(null, false, "");
	private Object2BooleanRBTreeMap<String> structureIcons = new Object2BooleanRBTreeMap<>(String::compareTo);

	public GlobalConfig() {}

	private GlobalConfig(GlobalConfig globalConfig) {
		this.locale = globalConfig.locale;
		this.regionSelectionColor = globalConfig.regionSelectionColor;
		this.chunkSelectionColor = globalConfig.chunkSelectionColor;
		this.pasteChunksColor = globalConfig.pasteChunksColor;
		this.processThreads = globalConfig.processThreads;
		this.writeThreads = globalConfig.writeThreads;
		this.maxLoadedFiles = globalConfig.maxLoadedFiles;
		this.mcSavesDir = globalConfig.mcSavesDir;
		this.showChunkGrid = globalConfig.showChunkGrid;
		this.showRegionGrid = globalConfig.showRegionGrid;
		this.showCoordinates = globalConfig.showCoordinates;
		this.coordinateStyle = globalConfig.coordinateStyle;
		this.debug = globalConfig.debug;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		Translation.load(locale);
	}

	public Color getRegionSelectionColor() {
		return regionSelectionColor;
	}

	public void setRegionSelectionColor(Color regionSelectionColor) {
		this.regionSelectionColor = regionSelectionColor;
	}

	public Color getChunkSelectionColor() {
		return chunkSelectionColor;
	}

	public void setChunkSelectionColor(Color chunkSelectionColor) {
		this.chunkSelectionColor = chunkSelectionColor;
	}

	public Color getPasteChunksColor() {
		return pasteChunksColor;
	}

	public void setPasteChunksColor(Color pasteChunksColor) {
		this.pasteChunksColor = pasteChunksColor;
	}

	public int getProcessThreads() {
		return processThreads;
	}

	public void setProcessThreads(int processThreads) {
		this.processThreads = processThreads;
	}

	public int getWriteThreads() {
		return writeThreads;
	}

	public void setWriteThreads(int writeThreads) {
		this.writeThreads = writeThreads;
	}

	public int getMaxLoadedFiles() {
		return maxLoadedFiles;
	}

	public void setMaxLoadedFiles(int maxLoadedFiles) {
		this.maxLoadedFiles = maxLoadedFiles;
	}

	public String getMcSavesDir() {
		return mcSavesDir;
	}

	public void setMcSavesDir(String mcSavesDir) {
		this.mcSavesDir = mcSavesDir;
	}

	public boolean showChunkGrid() {
		return showChunkGrid;
	}

	public void setShowChunkGrid(boolean showChunkGrid) {
		this.showChunkGrid = showChunkGrid;
	}

	public boolean showRegionGrid() {
		return showRegionGrid;
	}

	public void setShowRegionGrid(boolean showRegionGrid) {
		this.showRegionGrid = showRegionGrid;
	}

	public boolean showCoordinates() {
		return showCoordinates;
	}

	public void setShowCoordinates(boolean showCoordinates) {
		this.showCoordinates = showCoordinates;
	}

	public String getCoordinateStyle() {
		return coordinateStyle;
	}

	public void setCoordinateStyle(String coordinateStyle) {
		this.coordinateStyle = coordinateStyle;
	}

	public boolean getDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
		Logging.setLogLevel(debug ? Logging.DEBUG : Logging.WARN);
		Logging.updateThreadContext();
	}

	public TreeMap<Long, RecentWorld> getRecentWorlds() {
		return recentWorlds;
	}

	public void addRecentWorld(File world, List<File> dimensionDirectories) {
		if (world == null) {
			return;
		}
		// check if this file already exists in the list and only update the timestamp if it does
		for (Map.Entry<Long, RecentWorld> entry : recentWorlds.entrySet()) {
			if (entry.getValue().recentWorld.equals(world)) {
				recentWorlds.remove(entry.getKey());
				recentWorlds.put(System.currentTimeMillis(), new RecentWorld(world, dimensionDirectories));
				return;
			}
		}

		// if it doesn't exist, we need to make sure that we delete the oldest entry if there are already 10 entries
		if (recentWorlds.size() >= MAX_RECENT_FILES) {
			recentWorlds.remove(recentWorlds.firstKey());
		}

		recentWorlds.put(System.currentTimeMillis(), new RecentWorld(world, dimensionDirectories));
	}

	public RecentFiles getRecentFilterScripts() {
		return recentFilterScripts;
	}

	public void addRecentFilterScript(File file) {
		recentFilterScripts.addRecentFile(file);
	}

	public RecentFiles getRecentChangeScripts() {
		return recentChangeScripts;
	}

	public void addRecentChangeScript(File file) {
		recentChangeScripts.addRecentFile(file);
	}

	public RecentFiles getRecentOverlayScripts() {
		return recentOverlayScripts;
	}

	public List<ReplaceBlocksUserPreset> getReplaceBlocksUserPresets() {
		return replaceBlocksUserPresets;
	}

	public void addRecentOverlayScript(File file) {
		recentOverlayScripts.addRecentFile(file);
	}

	public TempScript getFilterScript() {
		return filterScript;
	}

	public void setFilterScript(TempScript filterScript) {
		this.filterScript = filterScript;
	}

	public TempScript getChangeScript() {
		return changeScript;
	}

	public void setChangeScript(TempScript changeScript) {
		this.changeScript = changeScript;
	}

	public TempScript getOverlayScript() {
		return overlayScript;
	}

	public void setOverlayScript(TempScript overlayScript) {
		this.overlayScript = overlayScript;
	}

	public Object2BooleanRBTreeMap<String> getStructureIcons() {
		return structureIcons;
	}

	public void setStructureIcons(Object2BooleanRBTreeMap<String> structureIcons) {
		this.structureIcons = structureIcons;
	}

	@Override
	public void save() {
		saveWithResult();
	}

	public boolean saveWithResult() {
		return saveWithResult(gsonInstance, BASE_CONFIG_FILE);
	}

	public static GlobalConfig load() {
		String json = loadString(BASE_CONFIG_FILE);
		if (json == null) {
			GlobalConfig newGlobalConfig = new GlobalConfig();
			String lang = MSWindowsRegistry.getValue("Language");
			Locale locale = LocaleAdapter.parse(lang);
			if (Translation.isLanguageAvailable(locale)) {
				newGlobalConfig.setLocale(locale);
			}
			return newGlobalConfig;
		}
		GlobalConfig cfg = gsonInstance.fromJson(json, GlobalConfig.class);
		cfg.setDebug(cfg.getDebug()); // trigger potential change in debug level
		StructureRegistry.forEachDisplayName((d, i) -> cfg.structureIcons.putIfAbsent(i.id(), true));
		return cfg;
	}

	private static final Gson toStringGsonInstance;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Color.class, new ColorAdapter());
		builder.registerTypeAdapter(File.class, new FileAdapter(BASE_DIR.getAbsolutePath()));
		builder.registerTypeAdapter(Locale.class, new LocaleAdapter());
		builder.serializeNulls();
		builder.excludeFieldsWithModifiers(Modifier.STATIC);
		builder.setFieldNamingStrategy(new GsonNamingStrategy());
		toStringGsonInstance = builder.create();
	}

	@Override
	public String toString() {
		return toStringGsonInstance.toJson(this);
	}

	public record RecentWorld(File recentWorld, List<File> dimensionDirectories) {

		private static final Pattern dimensionFolderPattern = Pattern.compile("^DIM-?\\d+$");
		private static final String[] parents = new String[]{"dimensions", "minecraft"};

		@Override
		public String toString() {
			// check if parent directories match world folder structure
			StringBuilder parentPath = new StringBuilder();
			File check = recentWorld.getParentFile();
			int i = parents.length - 1;
			for (; i >= -1; i--) {
				if (check == null) {
					break;
				}
				parentPath.insert(0, check.getName() + File.separator);
				if (i >= 0 && !check.getName().equals(parents[i])) {
					break;
				}
				check = check.getParentFile();
			}

			// this is dimensions/minecraft folder structure
			if (i < 0) {
				return parentPath + recentWorld.getName();
			}

			String name = recentWorld.getName();
			if (dimensionFolderPattern.matcher(name).matches()) {
				File parent = recentWorld.getParentFile();
				if (parent == null) {
					return name;
				}
				return parent.getName() + "/" + name;
			}
			return name;
		}
	}

	public GlobalConfig copyForSettings() {
		return new GlobalConfig(this);
	}

	public record TempScript(File file, boolean saved, String text) {}

	public record ReplaceBlocksUserPreset(String name, String value) {}
}
