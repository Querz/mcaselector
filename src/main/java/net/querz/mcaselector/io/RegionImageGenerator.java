package net.querz.mcaselector.io;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.FileHelper;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Progress;
import net.querz.mcaselector.util.Timer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RegionImageGenerator {

	private static Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	private RegionImageGenerator() {}

	public static void generate(Tile tile, Runnable callback, Supplier<Float> scaleSupplier, boolean force, boolean scaleOnly, Progress progressChannel) {
		setLoading(tile, true);
		MCAFilePipe.addJob(new MCAImageLoadJob(tile.getMCAFile(), tile, callback, scaleSupplier, force, scaleOnly, progressChannel));
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

		private Tile tile;
		private Runnable callback;
		private Supplier<Float> scaleSupplier;
		private boolean force, scaleOnly;
		private Progress progressChannel;

		MCAImageLoadJob(File file, Tile tile, Runnable callback, Supplier<Float> scaleSupplier, boolean force, boolean scaleOnly, Progress progressChannel) {
			super(file);
			this.tile = tile;
			this.callback = callback;
			this.scaleSupplier = scaleSupplier;
			this.force = force;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			if (!force) {
				tile.loadFromCache(callback, scaleSupplier);
			}

			if (!tile.isLoaded()) {
				byte[] data = load();
				if (data != null) {
					MCAFilePipe.executeProcessData(new MCAImageProcessJob(getFile(), data, tile, callback, scaleSupplier, scaleOnly, progressChannel));
					return;
				}
			}
			setLoading(tile, false);
			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}
		}

		public Tile getTile() {
			return tile;
		}
	}

	public static class MCAImageProcessJob extends ProcessDataJob {

		private Tile tile;
		private Runnable callback;
		private Supplier<Float> scaleSupplier;
		private boolean scaleOnly;
		private Progress progressChannel;

		MCAImageProcessJob(File file, byte[] data, Tile tile, Runnable callback, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
			super(file, data);
			this.tile = tile;
			this.callback = callback;
			this.scaleSupplier = scaleSupplier;
			this.scaleOnly = scaleOnly;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Image image = tile.generateImage(callback, getData());
			if (image != null) {
				MCAFilePipe.executeSaveData(new MCAImageSaveCacheJob(getFile(), image, tile, scaleSupplier, scaleOnly, progressChannel));
			} else {
				setLoading(tile, false);
				if (progressChannel != null) {
					progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
				}

			}
		}

		public Tile getTile() {
			return tile;
		}
	}

	public static class MCAImageSaveCacheJob extends SaveDataJob<Image> {

		private Tile tile;
		private Supplier<Float> scaleSupplier;
		private boolean scaleOnly;
		private Progress progressChannel;

		MCAImageSaveCacheJob(File file, Image data, Tile tile, Supplier<Float> scaleSupplier, boolean scaleOnly, Progress progressChannel) {
			super(file, data);
			this.tile = tile;
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
					int zoomLevel = Helper.getZoomLevel(scaleSupplier.get());
					File cacheFile = FileHelper.createPNGFilePath(new File(Config.getCacheDir().getAbsolutePath(), zoomLevel + ""), tile.getLocation());
					if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
						Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
					}

					BufferedImage scaled = Helper.scaleImage(img, (double) Tile.SIZE / (double) zoomLevel);
					ImageIO.write(scaled, "png", cacheFile);

				} else {
					for (int i = Helper.getMinZoomLevel(); i <= Helper.getMaxZoomLevel(); i *= 2) {
						File cacheFile = FileHelper.createPNGFilePath(new File(Config.getCacheDir().getAbsolutePath(), i + ""), tile.getLocation());
						if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
							Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
						}

						BufferedImage scaled = Helper.scaleImage(img, (double) Tile.SIZE / (double) i);
						ImageIO.write(scaled, "png", cacheFile);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			setLoading(tile, false);
			if (progressChannel != null) {
				progressChannel.incrementProgress(FileHelper.createMCAFileName(tile.getLocation()));
			}

			Debug.dumpf("took %s to cache image of %s to %s", t, tile.getMCAFile().getName(), FileHelper.createPNGFileName(tile.getLocation()));
		}
	}
}
