package net.querz.mcaselector.io2;

import javafx.application.Platform;
import javafx.scene.image.Image;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RegionImageCacheLoader {

	public static void load(Tile tile, TileMap tileMap) {
		tile.setLoading(true);
		tile.setLoaded(false);
		JobExecutor.addLoadDataJob(new RegionImageCacheLoadDataJob(tile, tileMap));
	}

	private static class RegionImageCacheLoadDataJob extends LoadDataJob {

		private Tile tile;
		private TileMap tileMap;

		public RegionImageCacheLoadDataJob(Tile tile, TileMap tileMap) {
			super(Helper.createPNGFilePath(tile.getLocation(), tileMap.getZoomLevel()));
			this.tile = tile;
			this.tileMap = tileMap;
		}

		@Override
		public void execute() {
			try (InputStream inputStream = new FileInputStream(get())) {
				tile.setImage(new Image(inputStream));
				tile.setLoaded(true);
				tile.setLoading(false);
				Platform.runLater(tileMap::update);
			} catch (IOException ex) {
				Debug.dump("region " + tile.getLocation() + " not cached");
				//do nothing
			}
		}
	}
}
