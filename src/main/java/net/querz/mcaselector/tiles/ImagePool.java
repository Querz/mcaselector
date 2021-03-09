package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.point.Point2i;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ImagePool {

	private final Map<Integer, LinkedHashMap<Point2i, Image>> pool = new HashMap<>();
	private final Set<Point2i> noMCA = new HashSet<>();
	private final TileMap tileMap;
	private final OverlayDataPool dataPool;

	private final double poolSize;

	// poolSize is a percentage indicating the amount of images cached in relation to the visible region
	public ImagePool(TileMap tileMap, OverlayDataPool dataPool, double poolSize) {
		// initialize pool
		int maxZoomLevel = Config.getMaxZoomLevel();
		for (int i = 0; i < maxZoomLevel; i++) {
			pool.put((int) Math.pow(2, i), new LinkedHashMap<>());
		}

		this.tileMap = tileMap;
		this.dataPool = dataPool;
		this.poolSize = poolSize;
	}

	public void requestImage(Tile tile, int scale) {
		if (noMCA.contains(tile.location)) {
			return;
		}

		if (RegionImageGenerator.isLoading(tile)) {
			// skip if we are already loading this tile
			return;
		}

		Debug.dumpf("requesting image for tile %s with scale %d (contains=%s, value=%s)", tile.location, scale, pool.get(scale).containsKey(tile.location), pool.get(scale).get(tile.location));

		// check if image exists in pool
		Image img = pool.get(scale).get(tile.location);
		if (img != null) {
			Debug.dumpf("image was cached in image pool: %d/%s", scale, tile.location);
			tile.setImage(img);
			tile.setLoaded(true);
			tile.setLoading(false);
			return;
		}

		// check if image exists in cache
		File cachedImgFile = FileHelper.createPNGFilePath(Config.getCacheDir(), scale, tile.location);
		if (cachedImgFile.exists()) {
			// load cached file

			tile.setLoading(true);

			Image cachedImg = new Image(cachedImgFile.toURI().toString(), true);
			cachedImg.progressProperty().addListener((v, o, n) -> {
				if (n.intValue() == 1 && !cachedImg.isError()) {

					Debug.dump("image loaded: " + cachedImgFile.getAbsolutePath());

					tile.setImage(cachedImg);
					tile.setLoaded(true);
					tile.setLoading(false);
					push(scale, tile.location, cachedImg);
					tileMap.update();
				}
			});
		} else {
			Debug.dump("image does not exist: " + cachedImgFile.getAbsolutePath());

			RegionImageGenerator.generate(tile, Config.getWorldUUID(), (i, u) -> {
				if (i == null) {
					noMCA.add(tile.location);
					return;
				}
				synchronized (Config.getWorldUUID()) {
					if (u.equals(Config.getWorldUUID())) {
						push(scale, tile.location, i);
						Debug.dumpf("pushed image for %s with scale %d to pool (contains=%s, value=%s)", tile.location, scale, pool.get(scale).containsKey(tile.location), pool.get(scale).get(tile.location));
						Platform.runLater(tileMap::update);
					}
				}
			},
			(l, u) -> {
				if (l == null) {
					return;
				}
				synchronized (Config.getWorldUUID()) {
					if (u.equals(Config.getWorldUUID())) {
						dataPool.push(tile.location, l);
						dataPool.requestImage(tile, tile.location, 0, 100000);
						Debug.dumpf("pushed data for %s to pool", tile.location);
						Platform.runLater(tileMap::update);
					}
				}
			},
			() -> (float) scale, false, null);
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
			Point2i removed = it.next();
			it.remove();
			Debug.dumpf("removed %s for scale %d from pool", removed, scale);
		}
	}

	public void clear() {
		for (Map.Entry<Integer, LinkedHashMap<Point2i, Image>> scale : pool.entrySet()) {
			scale.getValue().clear();
		}
		clearNoMCACache();
		Debug.dumpf("cleared pool");
	}

	public void discardImage(Point2i region) {
		for (Map.Entry<Integer, LinkedHashMap<Point2i, Image>> scale : pool.entrySet()) {
			scale.getValue().remove(region);
		}
		Debug.dumpf("removed images for %s from image pool", region);
		noMCA.remove(region);
	}

	public void clearNoMCACache() {
		noMCA.clear();
	}
}
