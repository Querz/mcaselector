package net.querz.mcaselector.io.job;

import javafx.scene.image.Image;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.Tile;
import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CachedImageLoadJob extends ProcessDataJob {

	private static final Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	public static void load(Tile tile, File cachedImageFile, int loadZoomLevel, int targetZoomLevel, Consumer<Image> callback) {
		JobHandler.addJob(new CachedImageLoadJob(tile, cachedImageFile, loadZoomLevel, targetZoomLevel, callback));
	}

	public static boolean isLoading(Tile tile) {
		return loading.contains(tile.getLocation());
	}

	public static void setLoading(Tile tile, boolean loading) {
		Debug.dumpf("set loading from cache for %s to %s, image:%s, loaded:%s",
			tile.getLocation(), loading, tile.getImage() == null ? "null" : tile.getImage().getHeight() + "x" + tile.getImage().getWidth(), tile.isLoaded());

		if (loading) {
			CachedImageLoadJob.loading.add(tile.getLocation());
		} else {
			CachedImageLoadJob.loading.remove(tile.getLocation());
		}
	}

	private Tile tile;
	private File cachedImageFile;
	private int loadZoomLevel, targetZoomLevel;
	private Consumer<Image> callback;

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
			Debug.dump("failed to load image from cache: " + cachedImgFile.getAbsolutePath());
			return null;
		}

		Debug.dump("image loaded: " + cachedImgFile.getAbsolutePath());

		if (loadZoomLevel != targetZoomLevel) {
			cachedImg = ImageHelper.scaleDownFXImage(cachedImg, Tile.SIZE / targetZoomLevel);
		}

		return cachedImg;
	}
}
