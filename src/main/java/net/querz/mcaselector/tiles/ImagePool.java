package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.RegionImageGenerator;
import net.querz.mcaselector.point.Point2i;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ImagePool {

	private Map<Integer, LinkedHashMap<Point2i, Image>> pool = new HashMap<>();
	private Set<Point2i> noMCA = new HashSet<>();
	private TileMap tileMap;

	private double poolSize;

	// poolSize is a percentage indicating the amount of images cached in relation to the visible region
	public ImagePool(TileMap tileMap, double poolSize) {
		// initialize pool
		int maxZoomLevel = Config.getMaxZoomLevel();
		for (int i = 0; i < maxZoomLevel; i++) {
			pool.put((int) Math.pow(2, i), new LinkedHashMap<>());
		}

		this.tileMap = tileMap;
		this.poolSize = poolSize;
	}

	public void requestImage(Tile tile, int scale) {

		// TODO:
//		if (!tile.location.equals(new Point2i(0, 0))) {
//			return;
//		}


		// check if we're allowed to request an image for this tile at this scale
//		if (tile.location.getX() % scale != 0 || tile.location.getY() % scale != 0) {
//			System.out.printf("not allowed to request an image for %s at scale %d\n", tile.location, scale);
//			return;
//		}

		if (noMCA.contains(tile.location)) {
			return;
		}

		tile.setLoading(true);

		// check if image exists in pool
		Image img = pool.get(scale).get(tile.location);
		if (img != null) {
			System.out.printf("image was cached: %d/%s\n", scale, tile.location);
			tile.setImage(img);
			tile.setLoaded(true);
			tile.setLoading(false);
			return;
		}

		// check if image exists in cache
		File cachedImgFile = FileHelper.createPNGFilePath(Config.getCacheDir(), scale, tile.location);
//		File cachedImgFile = new File("/Users/rb/IdeaProjects/mcaselector/cache/8ffb5342340f3964a246f3e05f38a7c1/" + scale + "/r.0.0.png");
		if (cachedImgFile.exists()) {
			// load cached file

			Image cachedImg = new Image(cachedImgFile.toURI().toString(), true);
			cachedImg.progressProperty().addListener((v, o, n) -> {
				if (n.intValue() == 1 && !cachedImg.isError()) {
					System.out.println("image loaded, updating tile map");
					tile.setImage(cachedImg);
					tile.setLoaded(true);
					tile.setLoading(false);
					push(scale, tile.location, cachedImg);
					tileMap.update();
				}
			});
		} else {
			System.out.println("cached image does not exist: " + cachedImgFile.getAbsolutePath());

			RegionImageGenerator.generate(tile, i -> {
				if (i == null) {
					noMCA.add(tile.location);
					return;
				}
				push(scale, tile.location, i);
				Platform.runLater(() -> tileMap.update());
			}, null, false, false, null);
		}
	}

	private void push(int scale, Point2i location, Image img) {
		pool.get(scale).put(location, img);
		trim(scale);
	}

	private void trim(int scale) {
		LinkedHashMap<Point2i, Image> scaleEntry = pool.get(scale);
		if (scaleEntry.size() <= tileMap.getVisibleTiles() * poolSize) {
			return;
		}
		Iterator<Point2i> it = scaleEntry.keySet().iterator();
		while (it.hasNext() && scaleEntry.size() > tileMap.getVisibleTiles() * poolSize) {
			it.next();
			it.remove();
		}
	}

	public void clear() {
		for (Map.Entry<Integer, LinkedHashMap<Point2i, Image>> scale : pool.entrySet()) {
			scale.getValue().clear();
		}
		clearNoMCACache();
	}

	public void clearNoMCACache() {
		noMCA.clear();
	}
}
