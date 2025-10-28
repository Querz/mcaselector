package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.config.WorldConfig;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.io.db.CacheHandler;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.tile.TileImage;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.util.progress.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RegionImageGenerator {

	private static final Logger LOGGER = LogManager.getLogger(RegionImageGenerator.class);

	private static final Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	private static final LinkedHashMap<Point2i, RegionMCAFile> cachedMCAFiles = new LinkedHashMap<>();
	private static Function<Point2i, Boolean> cacheEligibilityChecker = null;

	private static final Object cacheLock = new Object();

	private RegionImageGenerator() {}

	public static void generate(Tile tile, ImageGeneratorCallback callback, Integer zoomLevel, Progress progressChannel, boolean canSkipSaving, boolean structuresOnly, Supplier<Integer> prioritySupplier) {
		LOGGER.debug("adding job {}, tile:{}, scale:{}, loading:{}, image:{}, loaded:{}",
			MCAImageProcessJob.class.getSimpleName(), tile.getLocation(), zoomLevel, isLoading(tile), tile.getImage() == null ? "null" : tile.getImage().getHeight() + "x" + tile.getImage().getWidth(), tile.isLoaded());
		JobHandler.addJob(new MCAImageProcessJob(tile, new UniqueID(), callback, zoomLevel, progressChannel, canSkipSaving, structuresOnly, prioritySupplier));
	}

	public static RegionMCAFile getCachedRegionMCAFile(Point2i region) {
		synchronized (cacheLock) {
			return cachedMCAFiles.get(region);
		}
	}

	public static void cacheRegionMCAFile(RegionMCAFile regionMCAFile, UniqueID uniqueID) {
		synchronized (cacheLock) {
			if (!uniqueID.matchesCurrentConfig()) {
				return;
			}
			if (cacheEligibilityChecker != null && cacheEligibilityChecker.apply(regionMCAFile.getLocation())) {
				if (!cachedMCAFiles.containsKey(regionMCAFile.getLocation())) {
					if (cachedMCAFiles.size() >= ConfigProvider.GLOBAL.getMaxLoadedFiles()) {
						cachedMCAFiles.remove(cachedMCAFiles.keySet().iterator().next());
					}
					cachedMCAFiles.put(regionMCAFile.getLocation(), regionMCAFile.minimizeForRendering());
				}
			}
		}
	}

	public static void uncacheRegionMCAFile(Point2i region) {
		synchronized (cacheLock) {
			cachedMCAFiles.remove(region);
		}
	}

	public static void setCacheEligibilityChecker(Function<Point2i, Boolean> checker) {
		RegionImageGenerator.cacheEligibilityChecker = checker;
	}

	public static void invalidateCachedMCAFiles() {
		synchronized (cacheLock) {
			cachedMCAFiles.clear();
		}
	}

	public static boolean isLoading(Tile tile) {
		return loading.contains(tile.getLocation());
	}

	public static void setLoading(Tile tile, boolean loading) {
		LOGGER.debug("set loading from mca for {} to {}, image:{}, loaded:{}",
			tile.getLocation(), loading, tile.getImage() == null ? "null" : tile.getImage().getHeight() + "x" + tile.getImage().getWidth(), tile.isLoaded());

		if (loading) {
			RegionImageGenerator.loading.add(tile.getLocation());
		} else {
			RegionImageGenerator.loading.remove(tile.getLocation());
		}
	}

	public static class UniqueID {

		private final UUID world;
		private final int height;
		private final boolean layerOnly;
		private final boolean shade;
		private final boolean shadeWater;
		private final boolean caves;

		private UniqueID() {
			WorldConfig worldConfig = ConfigProvider.WORLD;
			this.world = worldConfig.getWorldUUID();
			this.height = worldConfig.getRenderHeight();
			this.layerOnly = worldConfig.getRenderLayerOnly();
			this.shade = worldConfig.getShade();
			this.shadeWater = worldConfig.getShadeWater();
			this.caves = worldConfig.getRenderCaves();
		}

		public boolean matchesCurrentConfig() {
			WorldConfig worldConfig = ConfigProvider.WORLD;
			return world.equals(worldConfig.getWorldUUID())
					&& height == worldConfig.getRenderHeight()
					&& layerOnly == worldConfig.getRenderLayerOnly()
					&& shade == worldConfig.getShade()
					&& shadeWater == worldConfig.getShadeWater()
					&& caves == worldConfig.getRenderCaves();
		}
	}

	public static class MCAImageProcessJob extends ProcessDataJob {

		private final Tile tile;
		private final UniqueID uniqueID;
		private final ImageGeneratorCallback callback;
		private final Integer zoomLevel;
		private final Progress progressChannel;
		private final boolean canSkipSaving;
		private final boolean structuresOnly;
		private final Supplier<Integer> prioritySupplier;

		private MCAImageProcessJob(Tile tile, UniqueID uniqueID, ImageGeneratorCallback callback, Integer zoomLevel, Progress progressChannel, boolean canSkipSaving, boolean structuresOnly, Supplier<Integer> prioritySupplier) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), PRIORITY_LOW);
			this.tile = tile;
			this.uniqueID = uniqueID;
			this.callback = callback;
			this.zoomLevel = zoomLevel;
			this.progressChannel = progressChannel;
			this.canSkipSaving = canSkipSaving;
			this.structuresOnly = structuresOnly;
			this.prioritySupplier = prioritySupplier;
			if (progressChannel != null) {
				errorHandler = t -> progressChannel.incrementProgress("error");
			}
		}

		@Override
		public boolean execute() {
			RegionMCAFile cachedRegion = getCachedRegionMCAFile(tile.getLocation());

			LOGGER.debug("generating image for {}", tile.getMCAFile().getAbsolutePath());

			File file = tile.getMCAFile();
			boolean isCached = false;
			if (cachedRegion == null) {
				cachedRegion = new RegionMCAFile(file);
				try {
					Timer t = new Timer();
					cachedRegion.load(true);
					LOGGER.debug("took {} to read mca file {}", t, cachedRegion.getFile().getName());
				} catch (IOException ex) {
					LOGGER.warn("failed to load mca file {}", cachedRegion.getFile().getName());
					callback.accept(null, null, uniqueID);
					if (progressChannel != null) {
						progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
					}
					return true;
				}
			} else {
				isCached = true;
			}

			if (zoomLevel == null) {
				for (int z = Config.MIN_ZOOM_LEVEL; z <= Config.MAX_ZOOM_LEVEL; z *= 2) {
					Timer t = new Timer();
					Image image = TileImage.generateImage(cachedRegion, z);
					LOGGER.debug("took {} to generate image for region {} (zoom level {})", t, tile.getLocation(), z);

					callback.accept(image, null, uniqueID);

					// don't cache in memory, we only want the file cache

					new MCAImageSaveCacheJob(image, null, tile, z, null, canSkipSaving).execute();
				}
				if (progressChannel != null) {
					progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
				}
			} else {
				Image image = null;
				if (!structuresOnly) {
					Timer t = new Timer();
					image = TileImage.generateImage(cachedRegion, zoomLevel);
					LOGGER.debug("took {} to generate image for region {}", t, tile.getLocation());
				}

				Long2ObjectOpenHashMap<String[]> structures = cachedRegion.getStructures();

				callback.accept(image, structures, uniqueID);

				cacheRegionMCAFile(cachedRegion, uniqueID);

				if ((image != null || structuresOnly) && !isCached) {
					MCAImageSaveCacheJob job = new MCAImageSaveCacheJob(image, structures, tile, zoomLevel, progressChannel, canSkipSaving);
					job.errorHandler = errorHandler;
					JobHandler.executeSaveData(job);
					return false;
				} else {
					if (progressChannel != null) {
						progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
					}
				}
			}
			return true;
		}

		@Override
		public void cancel() {
			LOGGER.debug("cancelling job {}, tile:{}, scale:{}, loading:{}, image:{}, loaded:{}",
				MCAImageProcessJob.class.getSimpleName(), tile.getLocation(), zoomLevel, isLoading(tile), tile.getImage() == null ? "null" : tile.getImage().getHeight() + "x" + tile.getImage().getWidth(), tile.isLoaded());

			setLoading(tile, false);

			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}
		}

		public Tile getTile() {
			return tile;
		}

		@Override
		public int getPriority() {
			if (prioritySupplier == null) {
				return super.getPriority();
			}
			return super.getBasePriority() + prioritySupplier.get();
		}
	}

	private static class MCAImageSaveCacheJob extends SaveDataJob<Image> {

		private final Long2ObjectOpenHashMap<String[]> structures;
		private final Tile tile;
		private final int zoomLevel;
		private final Progress progressChannel;
		private final boolean canSkip;

		private MCAImageSaveCacheJob(Image data, Long2ObjectOpenHashMap<String[]> structures, Tile tile, int zoomLevel, Progress progressChannel, boolean canSkip) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), data);
			this.structures = structures;
			this.tile = tile;
			this.zoomLevel = zoomLevel;
			this.progressChannel = progressChannel;
			this.canSkip = canSkip;
		}

		@Override
		public void execute() {
			Timer t = new Timer();

			// save image to cache
			if (getData() != null) {
				try {
					BufferedImage img = SwingFXUtils.fromFXImage(getData(), null);
					File cacheFile = FileHelper.createPNGFilePath(ConfigProvider.WORLD.getCacheDir(zoomLevel), tile.getLocation());
					if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
						LOGGER.warn("failed to create cache directory for {}", cacheFile.getAbsolutePath());
					}
					LOGGER.debug("writing cache file {}", cacheFile.getAbsolutePath());
					ImageIO.write(img, "png", cacheFile);
				} catch (IOException ex) {
					LOGGER.warn("failed to save images to cache for {}", tile.getLocation(), ex);
				}

				LOGGER.debug("took {} to cache image of {} to {}", t, tile.getMCAFile().getName(), FileHelper.createPNGFileName(tile.getLocation()));
			}

			if (structures != null) {
				try {
					CacheHandler.setStructureData(tile.getLocation(), structures);
				} catch (IOException e) {
					LOGGER.warn("failed to cache structure data for {}", tile.getLocation(), e);
				}
			}

			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}


			done();
		}

		@Override
		public void cancel() {
			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}
		}

		@Override
		public boolean canSkip() {
			return canSkip;
		}
	}

	@FunctionalInterface
	public interface ImageGeneratorCallback {
		void accept(Image image, Long2ObjectOpenHashMap<String[]> structures, UniqueID uniqueID);
	}
}
