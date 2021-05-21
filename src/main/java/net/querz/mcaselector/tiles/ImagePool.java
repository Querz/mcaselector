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

	private final double poolSize;

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
		if (noMCA.contains(tile.location)) {
			tile.setImage(null);
			tile.setLoaded(true);
			return;
		}

		// skip if this tile is already loading
		if (RegionImageGenerator.isLoading(tile)) {
			return;
		}

		// check if image exists in pool
		Image img = pool.get(scale).get(tile.location);
		if (img != null) {
			Debug.dumpf("image was cached in image pool: %d/%s", scale, tile.location);
			tile.setImage(img);
			tile.setLoaded(true);
			return;
		}

		// check if image exists in cache
		File cachedImgFile = FileHelper.createPNGFilePath(Config.getCacheDir(), scale, tile.location);
		if (cachedImgFile.exists()) {
			// load cached file
			loadImageFromDiskCache(tile, cachedImgFile, scale);

		} else if (RegionImageGenerator.isSaving(tile) && !RegionImageGenerator.hasActionOnSave(tile)) {
			// wait for saving to finish, then pull cached image from disk
			RegionImageGenerator.setOnSaved(tile, () -> loadImageFromDiskCache(tile, cachedImgFile, scale));

		} else {
			Debug.dump("image does not exist: " + cachedImgFile.getAbsolutePath());

			RegionImageGenerator.setLoading(tile, true);
			RegionImageGenerator.generate(tile, (i, u) -> Platform.runLater(() -> {
				RegionImageGenerator.setLoading(tile, false);
				if (u.matchesCurrentConfig()) {
					// check if scale is still correct
					tile.setImage(i);
					tile.setLoaded(true);
					if (i == null) {
						Debug.dumpf("image of %s is null", tile.getLocation());
						noMCA.add(tile.location);
						return;
					}
					push(scale, tile.location, i);
					tileMap.update();
				}
			}),
			() -> (float) scale, false, null, true);
		}
	}

	private void loadImageFromDiskCache(Tile tile, File cachedImgFile, int scale) {
		RegionImageGenerator.setLoading(tile, true);

		Image cachedImg = new Image(cachedImgFile.toURI().toString(), true);
		cachedImg.progressProperty().addListener((v, o, n) -> {
			if (n.intValue() == 1) {
				// run the following on the JavaFX main thread because concurrency
				Platform.runLater(() -> {
					RegionImageGenerator.setLoading(tile, false);
					if (cachedImg.isError()) {
						tile.setImage(null);
						tile.setLoaded(true);
						Debug.dump("failed to load image from cache: " + cachedImgFile.getAbsolutePath());
						return;
					}

					Debug.dump("image loaded: " + cachedImgFile.getAbsolutePath());

					tile.setImage(cachedImg);
					tile.setLoaded(true);
					push(scale, tile.location, cachedImg);
					tileMap.update();
				});
			}
		});
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

	public boolean hasNoMCA(Point2i p) {
		return noMCA.contains(p);
	}
}
