package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.io.NamedThreadFactory;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.point.Point2i;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ImagePool {

	private final Map<Integer, LinkedHashMap<Point2i, Image>> pool = new ConcurrentHashMap<>();
	private final Set<Point2i> noMCA = new HashSet<>();
	private final Set<Point2i> noCache = new HashSet<>();
	private final TileMap tileMap;

	private final double poolSize;

	// used to scale existing images asynchronously
	private final ThreadPoolExecutor imageScaler = new ThreadPoolExecutor(
		1, 1,
		0L, TimeUnit.MILLISECONDS,
		new LinkedBlockingQueue<>(),
		new NamedThreadFactory("imageScaler"));

	// used to check the file system if cache files exist
	private final ThreadPoolExecutor cacheChecker = new ThreadPoolExecutor(
		1, 1,
		0L, TimeUnit.MILLISECONDS,
		new LinkedBlockingQueue<>(),
		new NamedThreadFactory("cacheChecker"));

	// poolSize is a percentage indicating the amount of images cached in relation to the visible region
	public ImagePool(TileMap tileMap, double poolSize) {
		// initialize pool
		int maxZoomLevel = Config.getMaxZoomLevel();
		for (int i = 1; i <= maxZoomLevel; i *= 2) {
			pool.put(i, new LinkedHashMap<>());
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

		// check if image or a lower resolution version exists in the pool
		boolean inPool = false;
		for (int i = scale; i <= Config.getMaxZoomLevel(); i *= 2) {
			Image img = pool.get(i).get(tile.location);
			if (img != null) {
				Debug.dumpf("image was cached in image pool: %d/%s", i, tile.location);
				tile.setImage(img);
				if (i == scale) {
					tile.setLoaded(true);
					return;
				}
				inPool = true;
				break;
			}
		}

		// scale image down instead if tile already has an image, that's faster
		if (tile.image != null && tile.getImageZoomLevel() < scale) {
			RegionImageGenerator.setLoading(tile, true);
			scaleImageAsync(tile, tile.image, scale);
			return;
		}

		if (!noCache.contains(tile.location) && !inPool) {
			// check if this image or a lower resolution version of this image exists in cache
			noCache.add(tile.location);
			loadAnyFromDiskCacheAsync(tile, scale);
			return;
		}

		File cachedImgFile = FileHelper.createPNGFilePath(Config.getCacheDir(), scale, tile.location);

		if (RegionImageGenerator.isSaving(tile) && !RegionImageGenerator.hasActionOnSave(tile)) {
			// wait for saving to finish, then pull cached image from disk
			RegionImageGenerator.setOnSaved(tile, () -> loadImageFromDiskCache(tile, cachedImgFile, scale));

		} else {
			Debug.dump("image does not exist: " + cachedImgFile.getAbsolutePath());

			RegionImageGenerator.setLoading(tile, true);
			noCache.remove(tile.location);
			RegionImageGenerator.generate(tile, (i, u) ->  {
				RegionImageGenerator.setLoading(tile, false);
				if (u.matchesCurrentConfig()) {
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
			},
			scale, false, null, true);
		}
	}

	private void scaleImageAsync(Tile tile, Image image, int scale) {
		imageScaler.execute(() -> {
			Image scaled = SwingFXUtils.toFXImage(ImageHelper.scaleImage(SwingFXUtils.fromFXImage(image, null), (double) Tile.SIZE / scale), null);
			RegionImageGenerator.setLoading(tile, false);
			tile.image = scaled;
			tile.setLoaded(true);
			push(scale, tile.location, scaled);
			tileMap.update();
		});
	}

	private void loadAnyFromDiskCacheAsync(Tile tile, int scale) {
		cacheChecker.execute(() -> {
			for (int i = scale; i <= Config.getMaxZoomLevel(); i *= 2) {
				File cachedImgFile = FileHelper.createPNGFilePath(Config.getCacheDir(), i, tile.location);
				if (cachedImgFile.exists()) {
					// load cached file
					loadImageFromDiskCache(tile, cachedImgFile, i);
					if (scale == i) {
						noCache.remove(tile.location);
					}
					return;
				}
			}
			tileMap.update();
		});
	}

	private void loadImageFromDiskCache(Tile tile, File cachedImgFile, int scale) {
		RegionImageGenerator.setLoading(tile, true);

		Image cachedImg = new Image(cachedImgFile.toURI().toString(), true);
		cachedImg.progressProperty().addListener((v, o, n) -> {
			if (n.intValue() == 1) {
				// run the following on the JavaFX main thread because concurrency
				RegionImageGenerator.setLoading(tile, false);
				if (cachedImg.isError()) {
					// don't set image to null, we might already have an image
					tile.setLoaded(true);
					Debug.dump("failed to load image from cache: " + cachedImgFile.getAbsolutePath());
					return;
				}

				Debug.dump("image loaded: " + cachedImgFile.getAbsolutePath());

				tile.setImage(cachedImg);
				tile.setLoaded(true);
				push(scale, tile.location, cachedImg);
				tileMap.update();
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

		Set<Point2i> l = tileMap.getVisibleRegions(scale * 2);

		Iterator<Point2i> it = scaleEntry.keySet().iterator();

		while (it.hasNext()) {
			Point2i p = it.next();
			if (!l.contains(p)) {
				it.remove();
				Debug.dumpf("removed %s for scale %d from pool", p, scale);
			}
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

	public void dumpMetrics() {
		Debug.dumpf("ImagePool: pool1=%d, pool2=%d, pool4=%d, pool8=%d, noMCA=%d", pool.get(1).size(), pool.get(2).size(), pool.get(4).size(), pool.get(8).size(), noMCA.size());
		for (Map.Entry<Integer, LinkedHashMap<Point2i, Image>> entry : pool.entrySet()) {
			Debug.dumpf("pool%d:", entry.getKey());
			for (Map.Entry<Point2i, Image> cache : entry.getValue().entrySet()) {
				Debug.dumpf("  %s: %dx%d", cache.getKey(), (int) cache.getValue().getWidth(), (int) cache.getValue().getHeight());
			}
		}
	}

	public void mark(int scale) {
		LinkedHashMap<Point2i, Image> scaleEntry = pool.get(scale);
		Map<Point2i, Set<Point2i>> marked = new HashMap<>();
		for (Map.Entry<Point2i, Image> cache : scaleEntry.entrySet()) {
			marked.put(cache.getKey(), null);
		}

		tileMap.setMarkedChunks(marked);
		tileMap.update();
	}
}
