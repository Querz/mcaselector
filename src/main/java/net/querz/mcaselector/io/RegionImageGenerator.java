package net.querz.mcaselector.io;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Timer;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RegionImageGenerator {

	private static Set<Point2i> loading = new HashSet<>();

	private RegionImageGenerator() {}

	public static void generate(Tile tile, TileMap tileMap) {
		tile.setLoading(true);
		loading.add(tile.getLocation());
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
				}
			} else {
				tile.setLoading(false);
				loading.remove(tile.getLocation());
			}
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
				MCAFilePipe.executeSaveData(new MCAImageSaveCacheJob(getFile(), image, tile));
			} else {
				tile.setLoading(false);
				loading.remove(tile.getLocation());
			}
		}

		public Tile getTile() {
			return tile;
		}
	}

	public static class MCAImageSaveCacheJob extends SaveDataJob<Image> {

		private Tile tile;

		MCAImageSaveCacheJob(File file, Image data, Tile tile) {
			super(file, data);
			this.tile = tile;
		}

		@Override
		public void execute() {
			Timer t = new Timer();

			//save image to cache
			File cacheFile = tile.getCacheFile();
			if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
				Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
			}
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(getData(), null), "png", cacheFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			tile.setLoading(false);
			loading.remove(tile.getLocation());

			Debug.dumpf("took %s to cache image of %s to %s", t, tile.getMCAFile().getName(), cacheFile.getName());
		}
	}
}
