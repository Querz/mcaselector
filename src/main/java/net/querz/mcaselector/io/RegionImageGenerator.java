package net.querz.mcaselector.io;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RegionImageGenerator {

	private static final Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	private RegionImageGenerator() {}

	public static void generate(Tile tile, UUID world, BiConsumer<Image, UUID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
		setLoading(tile, true);
		MCAFilePipe.addJob(new MCAImageLoadJob(tile, world, callback, scaleSupplier, scaleOnly, progressChannel));
	}

	public static boolean isLoading(Tile tile) {
		return loading.contains(tile.getLocation());
	}

	public static void setLoading(Tile tile, boolean loading) {
		tile.setLoading(loading);
		if (loading) {
			RegionImageGenerator.loading.add(tile.getLocation());
		} else {
			RegionImageGenerator.loading.remove(tile.getLocation());
		}
	}

	public static class MCAImageLoadJob extends LoadDataJob {

		private final Tile tile;
		private final UUID world;
		private final BiConsumer<Image, UUID> callback;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;

		private MCAImageLoadJob(Tile tile, UUID world, BiConsumer<Image, UUID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
			super(new RegionDirectories(tile.getLocation(), null, null, null));
			this.tile = tile;
			this.world = world;
			this.callback = callback;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			if (!tile.isLoaded()) {
				byte[] data = load(tile.getMCAFile());
				if (data != null) {
					MCAFilePipe.executeProcessData(new MCAImageProcessJob(tile.getMCAFile(), data, tile, world, callback, scaleSupplier, scaleOnly, progressChannel));
					return;
				}
			}
			setLoading(tile, false);
			callback.accept(null, world);
			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}
		}

		public Tile getTile() {
			return tile;
		}
	}

	private static class MCAImageProcessJob extends ProcessDataJob {

		private final File file;
		private final Tile tile;
		private final UUID world;
		private final BiConsumer<Image, UUID> callback;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;

		private MCAImageProcessJob(File file, byte[] data, Tile tile, UUID world, BiConsumer<Image, UUID> callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), data, null, null);
			this.file = file;
			this.tile = tile;
			this.world = world;
			this.callback = callback;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Debug.dumpf("generating image for %s", file.getAbsolutePath());
			Image image = TileImage.generateImage(tile, world, callback, scaleSupplier, getRegionData());
			setLoading(tile, false);
			if (image != null) {
				MCAFilePipe.executeSaveData(new MCAImageSaveCacheJob(image, tile, world, scaleSupplier, scaleOnly, progressChannel));
			} else {
				if (progressChannel != null) {
					progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
				}
			}
		}

		public Tile getTile() {
			return tile;
		}
	}

	private static class MCAImageSaveCacheJob extends SaveDataJob<Image> {

		private final Tile tile;
		private final UUID world;
		private final Supplier<Float> scaleSupplier;
		private final boolean scaleOnly;
		private final Progress progressChannel;

		private MCAImageSaveCacheJob(Image data, Tile tile, UUID world, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
			super(new RegionDirectories(tile.getLocation(), null, null, null), data);
			this.tile = tile;
			this.world = world;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();

			//save image to cache
			try {
				BufferedImage img = SwingFXUtils.fromFXImage(getData(), null);
				if (scaleOnly) {
					int zoomLevel = Tile.getZoomLevel(scaleSupplier.get());
					File cacheFile = FileHelper.createPNGFilePath(Config.getCacheDirForWorldUUID(world, zoomLevel), tile.getLocation());
					if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
						Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
					}

					BufferedImage scaled = ImageHelper.scaleImage(img, (double) Tile.SIZE / (double) zoomLevel);
					Debug.dumpf("writing cache file %s", cacheFile.getAbsolutePath());
					ImageIO.write(scaled, "png", cacheFile);

				} else {
					for (int i = Config.getMinZoomLevel(); i <= Config.getMaxZoomLevel(); i *= 2) {
						File cacheFile = FileHelper.createPNGFilePath(Config.getCacheDirForWorldUUID(world, i), tile.getLocation());
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

			setLoading(tile, false);
			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}

			Debug.dumpf("took %s to cache image of %s to %s", t, tile.getMCAFile().getName(), FileHelper.createPNGFileName(tile.getLocation()));
		}
	}
}
