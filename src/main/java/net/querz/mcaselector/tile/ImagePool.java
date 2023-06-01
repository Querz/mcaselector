package net.querz.mcaselector.tile;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import javafx.scene.image.Image;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.io.db.CacheDBController;
import net.querz.mcaselector.io.job.CachedImageLoadJob;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.ui.dialog.ErrorDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public final class ImagePool {

	private static final Logger LOGGER = LogManager.getLogger(ImagePool.class);

	private final Object poolLock = new Object();
	private final Int2ObjectOpenHashMap<Long2ObjectLinkedOpenHashMap<Image>> pool = new Int2ObjectOpenHashMap<>(4);
	private final LongSet regions = new LongOpenHashSet(2048);
	private final TileMap tileMap;
	private final double poolSize;

	private final CacheDBController cache = CacheDBController.getInstance();

	// poolSize is a percentage indicating the amount of images cached in relation to the visible region
	public ImagePool(TileMap tileMap, double poolSize) {
		// initialize pool
		for (int i = 1; i <= Config.MAX_ZOOM_LEVEL; i *= 2) {
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
			tile.setImage(image);
			tile.setLoaded(true);
			return;
		}

		// try to get a higher res image for this tile from memory cache
		for (int zl = 1; zl <= Config.MAX_ZOOM_LEVEL; zl *= 2) {
			if (zl == zoomLevel) {
				continue;
			}

			// image is in memory cache and scale is right
			if ((image = pool.get(zl).get(tile.location.asLong())) != null) {

				if (zl < zoomLevel) {
					// image is larger than needed
					// scale down and set image to tile
					tile.setImage(ImageHelper.scaleDownFXImage(image, Tile.SIZE / zl));
					tile.setLoaded(true);
					push(zoomLevel, tile.location, tile.image);
					return;
				} else {
					// image is lower res, but we set it anyway, so we can at least display something
					tile.setImage(image);
					tile.setLoaded(true);
					// don't give up here, find image in disk cache!
					break;
				}
			}
		}

		// image in disk cache?
		File diskCacheImageFile = FileHelper.createPNGFilePath(ConfigProvider.WORLD.getCacheDir(), zoomLevel, tile.location);
		if (diskCacheImageFile.exists()) {
			CachedImageLoadJob.setLoading(tile, true);
			CachedImageLoadJob.load(tile, diskCacheImageFile, zoomLevel, zoomLevel, img -> {
				CachedImageLoadJob.setLoading(tile, false);
				push(zoomLevel, tile.location, img);
				tileMap.draw();
				if (isImageOutdated(tile.location)) {
					discardCachedImage(tile.location);
					tile.setLoaded(false);
				}
			});
			return;
		}

		for (int zl = 1; zl <= Config.MAX_ZOOM_LEVEL; zl *= 2) {
			if (zl == zoomLevel) {
				continue;
			}

			diskCacheImageFile = FileHelper.createPNGFilePath(ConfigProvider.WORLD.getCacheDir(), zl, tile.location);
			if (diskCacheImageFile.exists()) {
				if (zl < zoomLevel) {
					// image is larger than needed
					// load and scale down
					CachedImageLoadJob.setLoading(tile, true);
					CachedImageLoadJob.load(tile, diskCacheImageFile, zl, zoomLevel, img -> {
						CachedImageLoadJob.setLoading(tile, false);
						push(zoomLevel, tile.location, img);
						tileMap.draw();
						if (isImageOutdated(tile.location)) {
							discardCachedImage(tile.location);
							tile.setLoaded(false);
						}
					});
					return;
				} else {
					// image is lower res, but we load and set it anyway, so we can at least display something
					// load and set
					CachedImageLoadJob.setLoading(tile, true);
					CachedImageLoadJob.load(tile, diskCacheImageFile, zl, zl, img -> {
						CachedImageLoadJob.setLoading(tile, false);
						tileMap.draw();
						if (isImageOutdated(tile.location)) {
							discardCachedImage(tile.location);
							tile.setLoaded(false);
						}
					});
					break;
				}
			}
		}

		RegionImageGenerator.setLoading(tile, true);
		RegionImageGenerator.generate(tile, (img, uuid) -> {
			tile.setImage(img);
			tile.loaded = true;
			RegionImageGenerator.setLoading(tile, false);
			push(zoomLevel, tile.location, img);
			tileMap.draw();
			try {
				cache.setFileTime(tile.location, readLastModifiedDate(tile.location));
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}, zoomLevel, null, true, () -> tileMap.getTilePriority(tile.getLocation()));
	}

	public boolean isImageOutdated(Point2i region) {
		try {
			long time = cache.getFileTime(region);
			if (time == -1) {
				return false;
			}
			return time != readLastModifiedDate(region);
		} catch (SQLException e) {
			return false;
		}
	}

	private long readLastModifiedDate(Point2i region) {
		try {
			Path path = FileHelper.createMCAFilePath(region).toPath();
			if (!path.toFile().exists()) {
				return 0;
			}
			BasicFileAttributes bfa = Files.readAttributes(path, BasicFileAttributes.class);
			return bfa.lastModifiedTime().toMillis();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
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
				LOGGER.debug("removed {} for scale {} from pool", r, scale);
			}
		}
	}

	public void clear(ProgressTask task) {
		synchronized (poolLock) {
			for (Int2ObjectMap.Entry<Long2ObjectLinkedOpenHashMap<Image>> scale : pool.int2ObjectEntrySet()) {
				scale.getValue().clear();
			}
		}
		loadRegions(task);
		LOGGER.debug("cleared pool");
	}

	public void loadRegions(ProgressTask task) {
		regions.clear();

		if (task != null) {
			task.setMessage(Translation.DIALOG_PROGRESS_SCANNING_FILES.toString());
		}

		// get all files that match the "r.<x>.<z>.mca" name
		File[] files = ConfigProvider.WORLD.getWorldDirs().getRegion().listFiles();
		if (files == null) {
			return;
		}

		if (task != null) {
			task.setMax(files.length);
		}

		ForkJoinPool threadPool = new ForkJoinPool(ConfigProvider.GLOBAL.getProcessThreads());
		try {
			List<Point2i> points = threadPool.submit(() -> Arrays.stream(files).parallel()
					.filter(file -> file.length() > FileHelper.HEADER_SIZE) // only files that have more data than just the header
					.map(file -> {
						Point2i p = FileHelper.parseMCAFileName(file);
						if (task != null && p != null) {
							task.incrementProgress(String.format("%d, %d", p.getX(), p.getZ()));
						}
						return p;
					})
					.filter(Objects::nonNull)
					.toList()).get();
			points.forEach(p -> regions.add(p.asLong()));
			LOGGER.debug("loaded all world files");
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.warn("failed to load world", e);
			if (task != null) {
				task.done(null);
			}
			new ErrorDialog(tileMap.getWindow().getPrimaryStage(), e);
		} finally {
			threadPool.shutdown();
		}
	}

	public void discardImage(Point2i region) {
		synchronized (poolLock) {
			for (Int2ObjectMap.Entry<Long2ObjectLinkedOpenHashMap<Image>> scale : pool.int2ObjectEntrySet()) {
				scale.getValue().remove(region.asLong());
			}
		}
		if (!cache.isInitialized())
			return;
		try {
			cache.deleteData(region);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		LOGGER.debug("removed images for {} from image pool", region);
	}

	public void discardCachedImage(Point2i region) {
		discardImage(region);
		RegionImageGenerator.uncacheRegionMCAFile(region);
		for (int i = 1; i <= Config.MAX_ZOOM_LEVEL; i *= 2) {
			File png = FileHelper.createPNGFilePath(ConfigProvider.WORLD.getCacheDir(), i, region);
			png.delete();
		}
	}
}
