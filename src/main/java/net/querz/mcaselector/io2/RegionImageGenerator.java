package net.querz.mcaselector.io2;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

// this is explicitely used to generate images from mca files and write them to cache,
// NOT to load images from cache.
public class RegionImageGenerator {

	public static void generate(Tile tile, TileMap tileMap) {

	}

	public static class RegionImageGeneratorLoadDataJob extends LoadDataJob {

		private Tile tile;
		private TileMap tileMap;

		public RegionImageGeneratorLoadDataJob(Tile tile, TileMap tileMap) {
			super(tile.getMCAFile());
			this.tile = tile;
			this.tileMap = tileMap;
		}

		@Override
		public void execute() {
			byte[] data = loadBytes();

			if (data == null || data.length == 0) {
				tile.setLoading(false);
				tile.setLoaded(true);
				return;
			}

			Image image = Helper.generateImage(get(), data);
			if (image == null) {
				Debug.dumpf("failed to generate image of region %s", tile.getLocation());
				tile.setLoading(false);
				tile.setLoaded(true);
				return;
			}

			BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

			// resize and draw image to tile before writing cache files in background
			int currentZoomLevel = tileMap.getZoomLevel();
			if (currentZoomLevel > 1) {
				bufferedImage = Helper.scaleImage(bufferedImage, (double) (Tile.SIZE / currentZoomLevel));
				Point2i pointInCachedImage = Helper.getPointInCachedImage(tile.getLocation(), currentZoomLevel);
				tile.mergeImage(bufferedImage, pointInCachedImage);
			} else {
				// don't resize and merge if zoom level is 1
				tile.setImage(image);
			}

			tile.setLoading(false);
			tile.setLoaded(true);
			Platform.runLater(tileMap::update);

			// cache image for all zoom levels
			for (int z = Helper.getMinZoomLevel(); z <= Helper.getMaxZoomLevel(); z *= 2) {
				File cacheFile = Helper.createPNGFilePath(tile.getLocation(), z);
				JobExecutor.addSaveDataJob(new RegionImageGeneratorSaveDataJob(cacheFile, bufferedImage, tile, tileMap, z));
			}
		}
	}

	public static class RegionImageGeneratorSaveDataJob extends SaveDataJob<BufferedImage> {

		private Tile tile;
		private TileMap tileMap;
		private int zoomLevel;

		public RegionImageGeneratorSaveDataJob(File file, BufferedImage data, Tile tile, TileMap tileMap, int zoomLevel) {
			super(file, data);
			this.tile = tile;
			this.tileMap = tileMap;
			this.zoomLevel = zoomLevel;
		}

		@Override
		public void execute() {
			Point2i pointInCachedImage = Helper.getPointInCachedImage(tile.getLocation(), zoomLevel);
			BufferedImage scaledImage = Helper.scaleImage(getData(), (float) Tile.SIZE / (float) zoomLevel);
			BufferedImage cachedImage;

			if (get().exists()) {
				try {
					cachedImage = ImageIO.read(get());
				} catch (IOException ex) {
					Debug.errorf("could not read cache file %s to insert image of %s", get(), tile.getLocation());
					return;
				}
			} else {
				get().getParentFile().mkdirs();
				cachedImage = new BufferedImage(Tile.SIZE, Tile.SIZE, getData().getType());
			}

			Graphics graphics = cachedImage.getGraphics();

			Debug.dumpf("drawing %s in %i/%s at %s", tile.getLocation(), zoomLevel, get().getName(), pointInCachedImage);

			graphics.drawImage(scaledImage, pointInCachedImage.getX(), pointInCachedImage.getY(), null);

			try {
				ImageIO.write(cachedImage, "png", get());
			} catch (IOException ex) {
				Debug.errorf("could not write cache file %s to insert image of %s", get(), tile.getLocation());
				return;
			}
		}
	}
}
