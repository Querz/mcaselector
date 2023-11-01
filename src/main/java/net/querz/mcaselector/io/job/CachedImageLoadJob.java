package net.querz.mcaselector.io.job;

import javafx.scene.image.Image;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tile.Tile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CachedImageLoadJob extends ProcessDataJob {

	private static final Logger LOGGER = LogManager.getLogger(CachedImageLoadJob.class);

	private static final Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	public static void load(Tile tile, File cachedImageFile, int loadZoomLevel, int targetZoomLevel, Consumer<Image> callback) {
		JobHandler.addJob(new CachedImageLoadJob(tile, cachedImageFile, loadZoomLevel, targetZoomLevel, callback));
	}

	public static boolean isLoading(Tile tile) {
		return loading.contains(tile.getLocation());
	}

	public static void setLoading(Tile tile, boolean loading) {
		LOGGER.debug("set loading from cache for {} to {}, image:{}, loaded:{}",
			tile.getLocation(), loading, tile.getImage() == null ? "null" : tile.getImage().getHeight() + "x" + tile.getImage().getWidth(), tile.isLoaded());

		if (loading) {
			CachedImageLoadJob.loading.add(tile.getLocation());
		} else {
			CachedImageLoadJob.loading.remove(tile.getLocation());
		}
	}

	private final Tile tile;
	private final File cachedImageFile;
	private final int loadZoomLevel, targetZoomLevel;
	private final Consumer<Image> callback;

	public CachedImageLoadJob(Tile tile, File cachedImageFile, int loadZoomLevel, int targetZoomLevel, Consumer<Image> callback) {
		super(new RegionDirectories(tile.getLocation(), null, null, null), PRIORITY_MEDIUM);
		this.tile = tile;
		this.cachedImageFile = cachedImageFile;
		this.loadZoomLevel = loadZoomLevel;
		this.targetZoomLevel = targetZoomLevel;
		this.callback = callback;
	}

	@Override
	public boolean execute() {
		Image cachedImg = loadImageFromDiskCache(cachedImageFile);
		if (cachedImg != null) {
			tile.setImage(cachedImg);
		}
		tile.setLoaded(true);
		callback.accept(cachedImg);
		return true;
	}

	@Override
	public void cancel() {
		CachedImageLoadJob.setLoading(tile, false);
	}

	private Image loadImageFromDiskCache(File cachedImgFile) {
		Image cachedImg = new Image(cachedImgFile.toURI().toString(), false);

		if (cachedImg.isError()) {
			// don't set image to null, we might already have an image
			LOGGER.warn("failed to load image from cache: {}", cachedImgFile.getAbsolutePath());
			return null;
		}

		LOGGER.debug("image loaded: {}", cachedImgFile.getAbsolutePath());

		if (loadZoomLevel != targetZoomLevel) {
			cachedImg = ImageHelper.scaleDownFXImage(cachedImg, Tile.SIZE / targetZoomLevel);
		}

		return cachedImg;
	}
}
