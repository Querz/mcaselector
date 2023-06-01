package net.querz.mcaselector.config;

import com.google.gson.*;
import net.querz.mcaselector.config.adapter.FileAdapter;
import net.querz.mcaselector.config.adapter.WorldDirectoriesAdapter;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.logging.GsonNamingStrategy;
import net.querz.mcaselector.math.Bits;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.UUID;

public class WorldConfig extends Config {

	private static final Gson gsonInstance;

	static {
		GsonBuilder builder = new GsonBuilder();
		gsonInstance = builder.create();
	}

	// defaults
	public static final int DEFAULT_RENDER_HEIGHT = 319;
	public static final boolean DEFAULT_RENDER_LAYER_ONLY = false;
	public static final boolean DEFAULT_RENDER_CAVES = false;
	public static final boolean DEFAULT_SHADE = true;
	public static final boolean DEFAULT_SHADE_WATER = true;
	public static final boolean DEFAULT_SMOOTH_RENDERING = false;
	public static final boolean DEFAULT_SMOOTH_OVERLAYS = true;
	public static final String DEFAULT_TILEMAP_BACKGROUND = "BLACK";
	public static final boolean DEFAULT_SHOW_NONEXISTENT_REGIONS = true;

	// transient values
	private transient File regionDir = null;
	private transient WorldDirectories worldDirs = null;
	private transient UUID worldUUID = null;
	private transient File cacheDir = null;
	private transient List<File> dimensionDirectories = null;
	private transient File[] zoomLevelCacheDirs = null;

	// attributes
	private int renderHeight = DEFAULT_RENDER_HEIGHT;
	private boolean renderLayerOnly = DEFAULT_RENDER_LAYER_ONLY;
	private boolean renderCaves = DEFAULT_RENDER_CAVES;
	private boolean shade = DEFAULT_SHADE;
	private boolean shadeWater = DEFAULT_SHADE_WATER;
	private boolean smoothRendering = DEFAULT_SMOOTH_RENDERING;
	private boolean smoothOverlays = DEFAULT_SMOOTH_OVERLAYS;
	private String tileMapBackground = DEFAULT_TILEMAP_BACKGROUND;
	private boolean showNonexistentRegions = DEFAULT_SHOW_NONEXISTENT_REGIONS;

	private static final Logger LOGGER = LogManager.getLogger(WorldConfig.class);

	public File getRegionDir() {
		return regionDir;
	}

	public void setWorldDirs(WorldDirectories worldDirs) {
		this.worldDirs = worldDirs;
		this.worldUUID = UUID.nameUUIDFromBytes(worldDirs.getRegion().getAbsolutePath().getBytes());
		this.cacheDir = new File(BASE_CACHE_DIR, worldUUID.toString().replace("-", ""));
		this.regionDir = worldDirs.getRegion();
		this.zoomLevelCacheDirs = new File[Bits.lsbPosition(MAX_ZOOM_LEVEL) + 1];
		for (int i = MAX_ZOOM_LEVEL; i > 0; i >>= 1) {
			this.zoomLevelCacheDirs[Bits.lsbPosition(i)] = new File(cacheDir, "" + i);
		}
	}

	public WorldDirectories getWorldDirs() {
		return worldDirs;
	}

	public UUID getWorldUUID() {
		return worldUUID;
	}

	public File getCacheDir() {
		return cacheDir;
	}

	public File getCacheDir(int zoomLevel) {
		return zoomLevelCacheDirs[Bits.lsbPosition(zoomLevel)];
	}

	public File[] getCacheDirs() {
		return zoomLevelCacheDirs;
	}

	public void setCacheDir(File cacheDir) {
		this.cacheDir = new File(cacheDir, worldUUID.toString().replace("-", ""));
	}

	public List<File> getDimensionDirectories() {
		return dimensionDirectories;
	}

	public int getRenderHeight() {
		return renderHeight;
	}

	public void setRenderHeight(int renderHeight) {
		this.renderHeight = renderHeight;
	}

	public boolean getRenderLayerOnly() {
		return renderLayerOnly;
	}

	public void setRenderLayerOnly(boolean renderLayerOnly) {
		this.renderLayerOnly = renderLayerOnly;
	}

	public boolean getRenderCaves() {
		return renderCaves;
	}

	public void setRenderCaves(boolean renderCaves) {
		this.renderCaves = renderCaves;
	}

	public boolean getShade() {
		return shade;
	}

	public void setShade(boolean shade) {
		this.shade = shade;
	}

	public boolean getShadeWater() {
		return shadeWater;
	}

	public void setShadeWater(boolean shadeWater) {
		this.shadeWater = shadeWater;
	}

	public boolean getSmoothRendering() {
		return smoothRendering;
	}

	public void setSmoothRendering(boolean smoothRendering) {
		this.smoothRendering = smoothRendering;
	}

	public boolean getSmoothOverlays() {
		return smoothOverlays;
	}

	public void setSmoothOverlays(boolean smoothOverlays) {
		this.smoothOverlays = smoothOverlays;
	}

	public String getTileMapBackground() {
		return tileMapBackground;
	}

	public void setTileMapBackground(String tileMapBackground) {
		this.tileMapBackground = tileMapBackground;
	}

	public boolean getShowNonexistentRegions() {
		return showNonexistentRegions;
	}

	public void setShowNonexistentRegions(boolean showNonexistentRegions) {
		this.showNonexistentRegions = showNonexistentRegions;
	}

	@Override
	public void save() {
		save(gsonInstance, new File(cacheDir, "world_settings.json"));
	}

	public static WorldConfig load(WorldDirectories worldDirectories, List<File> dimensionDirectories) {
		LOGGER.debug("setting world directories to {}", worldDirectories);

		UUID worldUUID = UUID.nameUUIDFromBytes(worldDirectories.getRegion().getAbsolutePath().getBytes());
		File cacheDir = new File(BASE_CACHE_DIR, worldUUID.toString().replace("-", ""));

		String json = loadString(new File(cacheDir, "world_settings.json"));
		WorldConfig cfg;
		if (json == null) {
			cfg = new WorldConfig();
		} else {
			cfg = gsonInstance.fromJson(json, WorldConfig.class);
		}

		cfg.worldUUID = worldUUID;
		cfg.regionDir = worldDirectories.getRegion();
		cfg.worldDirs = worldDirectories;
		cfg.dimensionDirectories = dimensionDirectories;
		cfg.cacheDir = cacheDir;
		cfg.zoomLevelCacheDirs = new File[Bits.lsbPosition(MAX_ZOOM_LEVEL) + 1];
		for (int i = MAX_ZOOM_LEVEL; i > 0; i >>= 1) {
			cfg.zoomLevelCacheDirs[Bits.lsbPosition(i)] = new File(cacheDir, "" + i);
		}

		return cfg;
	}

	private static final Gson toStringGsonInstance;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithModifiers(Modifier.STATIC);
		builder.serializeNulls();
		builder.setFieldNamingStrategy(new GsonNamingStrategy());
		builder.registerTypeAdapter(File.class, new FileAdapter(BASE_DIR.getAbsolutePath()));
		builder.registerTypeAdapter(WorldDirectories.class, new WorldDirectoriesAdapter());
		toStringGsonInstance = builder.create();
	}

	@Override
	public String toString() {
		return toStringGsonInstance.toJson(this);
	}
}
