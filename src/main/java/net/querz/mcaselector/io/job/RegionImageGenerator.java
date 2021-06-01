package net.querz.mcaselector.io.job;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.RegionDirectories;
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
	private static final Set<Point2i> saving = ConcurrentHashMap.newKeySet();
	private static final Map<Point2i, Runnable> onSaved = new ConcurrentHashMap<>();

	private static final LinkedHashMap<Point2i, RegionMCAFile> cachedMCAFiles = new LinkedHashMap<>();
	private static Function<Point2i, Boolean> cacheEligibilityChecker = null;

	private RegionImageGenerator() {}

	public static void generate(Tile tile, BiConsumer<Image, UniqueID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel, boolean canSkipSaving) {
		MCAFilePipe.addJob(new MCAImageLoadJob(tile, new UniqueID(), callback, scaleSupplier, scaleOnly, progressChannel, canSkipSaving));
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
				if (cachedMCAFiles.size() > Config.getMaxLoadedFiles()) {
					cachedMCAFiles.entrySet().iterator().next();
				}
				if (!cachedMCAFiles.containsKey(regionMCAFile.getLocation())) {
					cachedMCAFiles.put(regionMCAFile.getLocation(), regionMCAFile);
				}
			}
		}
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
		if (loading) {
			RegionImageGenerator.loading.add(tile.getLocation());
		} else {
			RegionImageGenerator.loading.remove(tile.getLocation());
		}
	}

	private static void setSaving(Tile tile, boolean saving) {
		if (saving) {
			RegionImageGenerator.saving.add(tile.getLocation());
		} else {
			RegionImageGenerator.saving.remove(tile.getLocation());
		}
	}

	public static boolean isSaving(Tile tile) {
		return saving.contains(tile.getLocation());
	}

	public static void setOnSaved(Tile tile, Runnable action) {
		onSaved.put(tile.getLocation(), action);
	}

	public static boolean hasActionOnSave(Tile tile) {
		return onSaved.containsKey(tile.getLocation());
	}

	public static class UniqueID {

		private final UUID world;
		private final int height;
		private final boolean layerOnly;
		private final boolean shade;
		private final boolean shadeWater;

		private UniqueID() {
			this.world = Config.getWorldUUID();
			this.height = Config.getRenderHeight();
			this.layerOnly = Config.renderLayerOnly();
			this.shade = Config.shade();
			this.shadeWater = Config.shadeWater();
		}

		public boolean matchesCurrentConfig() {
			return world.equals(Config.getWorldUUID())
					&& height == Config.getRenderHeight()
					&& layerOnly == Config.renderLayerOnly()
					&& shade == Config.shade()
					&& shadeWater == Config.shadeWater();
		}
	}

	public static class MCAImageLoadJob extends LoadDataJob {

		private final Tile tile;
		private final UniqueID uniqueID;
		private final BiConsumer<Image, UniqueID> callback;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;
		private final boolean canSkipSaving;

		private MCAImageLoadJob(Tile tile, UniqueID uniqueID, BiConsumer<Image, UniqueID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel, boolean canSkipSaving) {
			super(new RegionDirectories(tile.getLocation(), null, null, null));
			this.tile = tile;
			this.uniqueID = uniqueID;
			this.callback = callback;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
			this.canSkipSaving = canSkipSaving;
		}

		@Override
		public void execute() {
			RegionMCAFile cachedRegion = getCachedRegionMCAFile(tile.getLocation());
			byte[] data = null;
			if (cachedRegion == null) {
				data = load(tile.getMCAFile());
			}
			if (data != null || cachedRegion != null) {
				MCAFilePipe.executeProcessData(new MCAImageProcessJob(tile.getMCAFile(), data, cachedRegion, tile, uniqueID, callback, scaleSupplier, scaleOnly, progressChannel, canSkipSaving));
				return;
			}
			callback.accept(null, uniqueID);
			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}
		}

		@Override
		public void cancel() {
			setLoading(tile, false);
		}

		public Tile getTile() {
			return tile;
		}
	}

	private static class MCAImageProcessJob extends ProcessDataJob {

		private final File file;
		private RegionMCAFile mcaFile;
		private final Tile tile;
		private final UniqueID uniqueID;
		private final BiConsumer<Image, UniqueID> callback;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;
		private final boolean canSkipSaving;

		private MCAImageProcessJob(File file, byte[] data, RegionMCAFile cachedFile, Tile tile, UniqueID uniqueID, BiConsumer<Image, UniqueID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel, boolean canSkipSaving) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), data, null, null);
			this.file = file;
			this.mcaFile = cachedFile;
			this.tile = tile;
			this.uniqueID = uniqueID;
			this.callback = callback;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
			this.canSkipSaving = canSkipSaving;
		}

		@Override
		public void execute() {
			Debug.dumpf("generating image for %s", file.getAbsolutePath());

			File file = tile.getMCAFile();
			ByteArrayPointer ptr = new ByteArrayPointer(getRegionData());
			boolean isCached = false;
			if (mcaFile == null) {
				mcaFile = new RegionMCAFile(file);
				try {
					Timer t = new Timer();
					mcaFile.load(ptr);
					Debug.dumpf("took %s to read mca file %s", t, mcaFile.getFile().getName());
				} catch (IOException ex) {
					Debug.dumpf("failed to load mca file %s", mcaFile.getFile().getName());
				}
			} else {
				isCached = true;
			}

			Image image = TileImage.generateImage(mcaFile);

			if (image != null) {
				BufferedImage img = SwingFXUtils.fromFXImage(image, null);
				int zoomLevel = Tile.getZoomLevel(scaleSupplier.get());
				BufferedImage scaled = ImageHelper.scaleImage(img, (double) Tile.SIZE / zoomLevel);
				Image scaledImage = SwingFXUtils.toFXImage(scaled, null);
				callback.accept(scaledImage, uniqueID);
			} else {
				callback.accept(null, uniqueID);
			}

			cacheRegionMCAFile(mcaFile, uniqueID);

			if (image != null && !isCached) {
				setSaving(tile, true);
				MCAFilePipe.executeSaveData(new MCAImageSaveCacheJob(image, tile, uniqueID, scaleSupplier, scaleOnly, progressChannel, canSkipSaving));
			} else {
				if (progressChannel != null) {
					progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
				}
			}
		}

		@Override
		public void cancel() {
			setLoading(tile, false);
		}

		public Tile getTile() {
			return tile;
		}
	}

	private static class MCAImageSaveCacheJob extends SaveDataJob<Image> {

		private final Tile tile;
		private final UniqueID uniqueID;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;
		private final boolean canSkip;

		private MCAImageSaveCacheJob(Image data, Tile tile, UniqueID uniqueID, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel, boolean canSkip) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), data);
			this.tile = tile;
			this.uniqueID = uniqueID;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
			this.canSkip = canSkip;
		}

		@Override
		public void execute() {
			Timer t = new Timer();

			// save image to cache
			try {
				BufferedImage img = SwingFXUtils.fromFXImage(getData(), null);
				if (scaleOnly) {
					int zoomLevel = Tile.getZoomLevel(scaleSupplier.get());
					File cacheFile = FileHelper.createPNGFilePath(Config.getCacheDirForWorldUUID(uniqueID.world, zoomLevel), tile.getLocation());
					if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
						Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
					}

					BufferedImage scaled = ImageHelper.scaleImage(img, (double) Tile.SIZE / (double) zoomLevel);
					Debug.dumpf("writing cache file %s", cacheFile.getAbsolutePath());
					ImageIO.write(scaled, "png", cacheFile);

				} else {
					for (int i = Config.getMinZoomLevel(); i <= Config.getMaxZoomLevel(); i *= 2) {
						File cacheFile = FileHelper.createPNGFilePath(Config.getCacheDirForWorldUUID(uniqueID.world, i), tile.getLocation());
						if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
							Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
						}

						BufferedImage scaled = ImageHelper.scaleImage(img, (double) Tile.SIZE / (double) i);
						Debug.dumpf("writing cache file %s", cacheFile.getAbsolutePath());
						ImageIO.write(scaled, "png", cacheFile);
					}
				}
			} catch (IOException ex) {
				Debug.dumpException("failed to save images to cache for " + tile.getLocation(), ex);
			}

			setSaving(tile, false);

			Runnable r = onSaved.get(tile.getLocation());
			if (r != null) {
				r.run();
				onSaved.remove(tile.getLocation());
			}

			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}

			Debug.dumpf("took %s to cache image of %s to %s", t, tile.getMCAFile().getName(), FileHelper.createPNGFileName(tile.getLocation()));
		}

		@Override
		public void cancel() {
			setSaving(tile, false);
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
