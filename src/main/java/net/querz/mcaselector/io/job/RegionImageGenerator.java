package net.querz.mcaselector.io.job;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileImage;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RegionImageGenerator {

	private static final Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	private static final LinkedHashMap<Point2i, RegionMCAFile> cachedMCAFiles = new LinkedHashMap<>();
	private static Function<Point2i, Boolean> cacheEligibilityChecker = null;

	private RegionImageGenerator() {}

	public static void generate(Tile tile, BiConsumer<Image, UniqueID> callback, int scale, Progress progressChannel, boolean canSkipSaving, Supplier<Integer> prioritySupplier) {
		Debug.dumpf("adding job %s, tile:%s, scale:%d, loading:%s, image:%s, loaded:%s",
			MCAImageProcessJob.class.getSimpleName(), tile.getLocation(), scale, isLoading(tile), tile.getImage() == null ? "null" : tile.getImage().getHeight() + "x" + tile.getImage().getWidth(), tile.isLoaded());
		JobHandler.addJob(new MCAImageProcessJob(tile, new UniqueID(), callback, scale, progressChannel, canSkipSaving, prioritySupplier));
	}

	public static RegionMCAFile getCachedRegionMCAFile(Point2i region) {
		return cachedMCAFiles.get(region);
	}

	public static void cacheRegionMCAFile(RegionMCAFile regionMCAFile, UniqueID uniqueID) {
		synchronized (cachedMCAFiles) {
			if (!uniqueID.matchesCurrentConfig()) {
				return;
			}
			if (cacheEligibilityChecker != null && cacheEligibilityChecker.apply(regionMCAFile.getLocation())) {
				if (!cachedMCAFiles.containsKey(regionMCAFile.getLocation())) {
					if (cachedMCAFiles.size() >= Config.getMaxLoadedFiles()) {
						cachedMCAFiles.remove(cachedMCAFiles.keySet().iterator().next());
					}
					cachedMCAFiles.put(regionMCAFile.getLocation(), regionMCAFile.minimizeForRendering());
				}
			}
		}
	}

	public static void uncacheRegionMCAFile(Point2i region) {
		cachedMCAFiles.remove(region);
	}

	public static void setCacheEligibilityChecker(Function<Point2i, Boolean> checker) {
		RegionImageGenerator.cacheEligibilityChecker = checker;
	}

	public static void invalidateCachedMCAFiles() {
		synchronized (cachedMCAFiles) {
			cachedMCAFiles.clear();
		}
	}

	public static boolean isLoading(Tile tile) {
		return loading.contains(tile.getLocation());
	}

	public static void setLoading(Tile tile, boolean loading) {
		Debug.dumpf("set loading from mca for %s to %s, image:%s, loaded:%s",
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
			this.world = Config.getWorldUUID();
			this.height = Config.getRenderHeight();
			this.layerOnly = Config.renderLayerOnly();
			this.shade = Config.shade();
			this.shadeWater = Config.shadeWater();
			this.caves = Config.renderCaves();
		}

		public boolean matchesCurrentConfig() {
			return world.equals(Config.getWorldUUID())
					&& height == Config.getRenderHeight()
					&& layerOnly == Config.renderLayerOnly()
					&& shade == Config.shade()
					&& shadeWater == Config.shadeWater()
					&& caves == Config.renderCaves();
		}
	}

	public static class MCAImageProcessJob extends ProcessDataJob {

		private final Tile tile;
		private final UniqueID uniqueID;
		private final BiConsumer<Image, UniqueID> callback;
		private final int scale;
		private final Progress progressChannel;
		private final boolean canSkipSaving;
		private final Supplier<Integer> prioritySupplier;

		private MCAImageProcessJob(Tile tile, UniqueID uniqueID, BiConsumer<Image, UniqueID> callback, int scale, Progress progressChannel, boolean canSkipSaving, Supplier<Integer> prioritySupplier) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), PRIORITY_LOW);
			this.tile = tile;
			this.uniqueID = uniqueID;
			this.callback = callback;
			this.scale = scale;
			this.progressChannel = progressChannel;
			this.canSkipSaving = canSkipSaving;
			this.prioritySupplier = prioritySupplier;
		}

		@Override
		public boolean execute() {
			RegionMCAFile cachedRegion = getCachedRegionMCAFile(tile.getLocation());
			byte[] data = null;
			if (cachedRegion == null) {
				data = load(tile.getMCAFile());
			}
			if (data == null && cachedRegion == null) {
				callback.accept(null, uniqueID);
				if (progressChannel != null) {
					progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
				}
				return true;
			}

			Debug.dumpf("generating image for %s", tile.getMCAFile().getAbsolutePath());

			File file = tile.getMCAFile();
			ByteArrayPointer ptr = new ByteArrayPointer(data);
			boolean isCached = false;
			if (cachedRegion == null) {
				cachedRegion = new RegionMCAFile(file);
				try {
					Timer t = new Timer();
					cachedRegion.load(ptr);
					Debug.dumpf("took %s to read mca file %s", t, cachedRegion.getFile().getName());
				} catch (IOException ex) {
					Debug.dumpf("failed to load mca file %s", cachedRegion.getFile().getName());
				}
			} else {
				isCached = true;
			}

			Image image = TileImage.generateImage(cachedRegion, scale);

			callback.accept(image, uniqueID);

			cacheRegionMCAFile(cachedRegion, uniqueID);

			if (image != null && !isCached) {
				JobHandler.executeSaveData(new MCAImageSaveCacheJob(image, tile, uniqueID, scale, progressChannel, canSkipSaving));
				return false;
			} else {
				if (progressChannel != null) {
					progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
				}
			}
			return true;
		}

		@Override
		public void cancel() {
			Debug.dumpf("cancelling job %s, tile:%s, scale:%d, loading:%s, image:%s, loaded:%s",
				MCAImageProcessJob.class.getSimpleName(), tile.getLocation(), scale, isLoading(tile), tile.getImage() == null ? "null" : tile.getImage().getHeight() + "x" + tile.getImage().getWidth(), tile.isLoaded());

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

		private final Tile tile;
		private final UniqueID uniqueID;
		private final int zoomLevel;
		private final Progress progressChannel;
		private final boolean canSkip;

		private MCAImageSaveCacheJob(Image data, Tile tile, UniqueID uniqueID, int zoomLevel, Progress progressChannel, boolean canSkip) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), data);
			this.tile = tile;
			this.uniqueID = uniqueID;
			this.zoomLevel = zoomLevel;
			this.progressChannel = progressChannel;
			this.canSkip = canSkip;
		}

		@Override
		public void execute() {
			Timer t = new Timer();

			// save image to cache
			try {
				BufferedImage img = SwingFXUtils.fromFXImage(getData(), null);
				File cacheFile = FileHelper.createPNGFilePath(Config.getCacheDirForWorldUUID(uniqueID.world, zoomLevel), tile.getLocation());
				if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
					Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
				}
				Debug.dumpf("writing cache file %s", cacheFile.getAbsolutePath());
				ImageIO.write(img, "png", cacheFile);
			} catch (IOException ex) {
				Debug.dumpException("failed to save images to cache for " + tile.getLocation(), ex);
			}

			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}

			Debug.dumpf("took %s to cache image of %s to %s", t, tile.getMCAFile().getName(), FileHelper.createPNGFileName(tile.getLocation()));

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
}
