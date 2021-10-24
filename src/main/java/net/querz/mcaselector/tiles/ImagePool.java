package net.querz.mcaselector.tiles;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.io.job.CachedImageLoadJob;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.point.Point2i;
import java.io.File;

public final class ImagePool {

	private final Object poolLock = new Object();
	private final Int2ObjectOpenHashMap<Long2ObjectLinkedOpenHashMap<Image>> pool = new Int2ObjectOpenHashMap<>(4);
	private final LongSet regions = new LongOpenHashSet(2048);
	private final TileMap tileMap;
	private final double poolSize;

	// poolSize is a percentage indicating the amount of images cached in relation to the visible region
	public ImagePool(TileMap tileMap, double poolSize) {
		// initialize pool
		int maxZoomLevel = Config.getMaxZoomLevel();
		for (int i = 1; i <= maxZoomLevel; i *= 2) {
			pool.put(i, new Long2ObjectLinkedOpenHashMap<>());
		}

		this.tileMap = tileMap;
		this.poolSize = poolSize;
	}

	// does stuff synchronously
	public void requestImage(Tile tile, int zoomLevel) {

		// we already know that there is no image in any cache if the mca file doesn't exist
		if (!regions.contains(tile.location.asLong())) {
			tile.setLoaded(true);
			return;
		}

		// if the image is already loading, we ignore it
		if (RegionImageGenerator.isLoading(tile) || CachedImageLoadJob.isLoading(tile)) {
			return;
		}

		// try to get the matching res image from memory cache
		Image image;
		if ((image = pool.get(zoomLevel).get(tile.location.asLong())) != null) {
			tile.image = image;
			tile.setLoaded(true);
			return;
		}

		// try to get a higher res image for this tile from memory cache
		for (int zl = 1; zl <= Config.getMaxZoomLevel(); zl *= 2) {
			if (zl == zoomLevel) {
				continue;
			}

			// image is in memory cache and scale is right
			if ((image = pool.get(zl).get(tile.location.asLong())) != null) {

				if (zl < zoomLevel) {
					// image is larger than needed
					// scale down and set image to tile
					tile.image = ImageHelper.scaleFXImage(image, Tile.SIZE / zl);
					tile.setLoaded(true);
					push(zoomLevel, tile.location, tile.image);
					return;
				} else {
					// image is lower res, but we set it anyway, so we can at least display something
					tile.image = image;
					tile.setLoaded(true);
					// don't give up here, find image in disk cache!
					break;
				}
			}
		}

		// image in disk cache?
		File diskCacheImageFile = FileHelper.createPNGFilePath(Config.getCacheDir(), zoomLevel, tile.location);
		if (diskCacheImageFile.exists()) {
			CachedImageLoadJob.setLoading(tile, true);
			CachedImageLoadJob.load(tile, diskCacheImageFile, zoomLevel, zoomLevel, img -> {
				CachedImageLoadJob.setLoading(tile, false);
				push(zoomLevel, tile.location, img);
				tileMap.draw();
			});
			return;
		}

		for (int zl = 1; zl <= Config.getMaxZoomLevel(); zl *= 2) {
			if (zl == zoomLevel) {
				continue;
			}

			diskCacheImageFile = FileHelper.createPNGFilePath(Config.getCacheDir(), zl, tile.location);
			if (diskCacheImageFile.exists()) {
				if (zl < zoomLevel) {
					// image is larger than needed
					// load and scale down
					CachedImageLoadJob.setLoading(tile, true);
					CachedImageLoadJob.load(tile, diskCacheImageFile, zl, zoomLevel, img -> {
						CachedImageLoadJob.setLoading(tile, false);
						push(zoomLevel, tile.location, img);
						tileMap.draw();
					});
					return;
				} else {
					// image is lower res, but we load and set it anyway, so we can at least display something
					// load and set
					CachedImageLoadJob.setLoading(tile, true);
					CachedImageLoadJob.load(tile, diskCacheImageFile, zl, zl, img -> {
						CachedImageLoadJob.setLoading(tile, false);
						tileMap.draw();
					});
					break;
				}
			}
		}

		RegionImageGenerator.setLoading(tile, true);
		RegionImageGenerator.generate(tile, (img, uuid) -> {
			tile.image = img;
			tile.loaded = true;
			RegionImageGenerator.setLoading(tile, false);
			push(zoomLevel, tile.location, img);
			tileMap.draw();
		}, zoomLevel, null, true);
	}

	private void push(int scale, Point2i location, Image img) {
		synchronized (poolLock) {
			pool.get(scale).put(location.asLong(), img);
			trim(scale);
		}
	}

	private void trim(int scale) {
		Long2ObjectLinkedOpenHashMap<Image> scaleEntry = pool.get(scale);
		if (scaleEntry.size() <= tileMap.getVisibleTiles() * poolSize) {
			return;
		}

		ObjectOpenHashSet<Point2i> l = tileMap.getVisibleRegions(scale * 2);

		LongBidirectionalIterator it = scaleEntry.keySet().iterator();

		while (it.hasNext()) {
			long p = it.nextLong();
			Point2i r = new Point2i(p);
			if (!l.contains(r)) {
				it.remove();
				Debug.dumpf("removed %s for scale %d from pool", r, scale);
			}
		}
	}

	public void clear() {
		synchronized (poolLock) {
			for (Int2ObjectMap.Entry<Long2ObjectLinkedOpenHashMap<Image>> scale : pool.int2ObjectEntrySet()) {
				scale.getValue().clear();
			}
		}
		loadRegions();
		Debug.dumpf("cleared pool");
	}

	public void loadRegions() {
		regions.clear();
		// get all files that match the "r.<x>.<z>.mca" name and that have more data than just the header
		File[] files = Config.getWorldDirs().getRegion().listFiles(f -> f.getName().matches(FileHelper.MCA_FILE_PATTERN) && f.length() > 8192);
		if (files == null) {
			return;
		}

		for (File file : files) {
			Point2i p = FileHelper.parseMCAFileName(file);
			regions.add(p.asLong());
		}
	}

	public void discardImage(Point2i region) {
		for (Int2ObjectMap.Entry<Long2ObjectLinkedOpenHashMap<Image>> scale : pool.int2ObjectEntrySet()) {
			scale.getValue().remove(region.asLong());
		}
		Debug.dumpf("removed images for %s from image pool", region);
	}

	public void dumpMetrics() {
		Debug.dumpf("ImagePool: pool1=%d, pool2=%d, pool4=%d, pool8=%d", pool.get(1).size(), pool.get(2).size(), pool.get(4).size(), pool.get(8).size());
		for (Int2ObjectMap.Entry<Long2ObjectLinkedOpenHashMap<Image>> entry : pool.int2ObjectEntrySet()) {
			Debug.dumpf("pool%d:", entry.getIntKey());
			for (Long2ObjectMap.Entry<Image> cache : entry.getValue().long2ObjectEntrySet()) {
				Debug.dumpf("  %s: %dx%d", new Point2i(cache.getLongKey()), cache.getValue() == null ? 0 : (int) cache.getValue().getWidth(),  cache.getValue() == null ? 0 : (int) cache.getValue().getHeight());
			}
		}
		Debug.dump("Regions:");
		for (Long region : regions) {
			Debug.dumpf("  %s", new Point2i(region));
		}
	}

	// marks tiles whose images are in the memory cache for a given scale. used for debugging.
	public void mark(int scale) {
		Long2ObjectLinkedOpenHashMap<Image> scaleEntry = pool.get(scale);
		Long2ObjectOpenHashMap<LongOpenHashSet> marked = new Long2ObjectOpenHashMap<>();
		for (Long2ObjectMap.Entry<Image> cache : scaleEntry.long2ObjectEntrySet()) {
			marked.put(cache.getLongKey(), null);
		}

		tileMap.setMarkedChunks(marked);
		tileMap.draw();
	}
}
