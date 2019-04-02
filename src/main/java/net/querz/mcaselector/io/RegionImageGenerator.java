package net.querz.mcaselector.io;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Timer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RegionImageGenerator {

	private static Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	private RegionImageGenerator() {}

	public static void generate(Tile tile, TileMap tileMap) {
		setLoading(tile, true);
		MCAFilePipe.addJob(new MCAImageLoadJob(tile.getMCAFile(), tile, tileMap));
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
		private TileMap tileMap;

		MCAImageLoadJob(File file, Tile tile, TileMap tileMap) {
			super(file);
			this.tile = tile;
			this.tileMap = tileMap;
		}

		@Override
		public void execute() {
			tile.loadFromCache(tileMap);

			if (!tile.isLoaded()) {
				byte[] data = load();
				if (data != null) {
					MCAFilePipe.executeProcessData(new MCAImageProcessJob(getFile(), data, tile, tileMap));
					return;
				}
			}
			setLoading(tile, false);
		}

		public Tile getTile() {
			return tile;
		}
	}

	public static class MCAImageProcessJob extends ProcessDataJob {

		private Tile tile;
		private TileMap tileMap;

		MCAImageProcessJob(File file, byte[] data, Tile tile, TileMap tileMap) {
			super(file, data);
			this.tile = tile;
			this.tileMap = tileMap;
		}

		@Override
		public void execute() {
			Image image = tile.generateImage(tileMap, getData());
			if (image != null) {
				MCAFilePipe.executeSaveData(new MCAImageSaveCacheJob(getFile(), image, tile, tileMap));
			} else {
				setLoading(tile, false);
			}
		}

		public Tile getTile() {
			return tile;
		}
	}

	public static class MCAImageSaveCacheJob extends SaveDataJob<Image> {

		private Tile tile;
		private TileMap tileMap;

		MCAImageSaveCacheJob(File file, Image data, Tile tile, TileMap tileMap) {
			super(file, data);
			this.tile = tile;
			this.tileMap = tileMap;
		}

		@Override
		public void execute() {
			Timer t = new Timer();

			//save image to cache
			try {
				BufferedImage img = SwingFXUtils.fromFXImage(getData(), null);
				for (int i = Helper.getMinZoomLevel(); i <= Helper.getMaxZoomLevel(); i *= 2) {
					File cacheFile = Helper.createPNGFilePath(new File(Config.getCacheDir().getAbsolutePath(), i + ""), Helper.blockToRegion(tile.getLocation()));
					if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
						Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
					}

					BufferedImage scaled = Helper.scaleImage(img, (double) Tile.SIZE / (double) i);
					ImageIO.write(scaled, "png", cacheFile);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			setLoading(tile, false);

			Debug.dumpf("took %s to cache image of %s to %s", t, tile.getMCAFile().getName(), Helper.createPNGFileName(Helper.blockToRegion(tile.getLocation())));
		}
	}
}
