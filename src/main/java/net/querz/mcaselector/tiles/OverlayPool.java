package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.db.CacheDBController;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import net.querz.mcaselector.tiles.overlay.OverlayType;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OverlayPool {

	private final TileMap tileMap;
	private final Set<Point2i> noData = new HashSet<>();
	private final ThreadPoolExecutor overlayCacheLoaders = new ThreadPoolExecutor(
			4, 4,
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>());;

	// when key present, but value null: no cache file
	// when key not present: look if region exists
	private final CacheDBController dataCache = new CacheDBController();
	private OverlayParser parser;

	public OverlayPool(TileMap tileMap) {
		this.tileMap = tileMap;
	}

	public void setType(OverlayType type) {
		this.parser = type == null ? null : type.instance();
	}

	public void requestImage(Tile tile, int min, int max) {
		// check if data for this region exists
		if (noData.contains(tile.location)) {
			return;
		}

		if (parser == null) {
			return;
		}

		tile.overlayLoading = true;

		OverlayParser parser = tileMap.getOverlayType().instance();

		overlayCacheLoaders.execute(() -> {
			int[] data = null;
			try {
				data = dataCache.getData(parser, null, tile.location);
			} catch (Exception ex) {
				Debug.dumpException("failed to load cached overlay data for region " + tile.location, ex);
			}

			if (data != null) {
				tile.overlay = parseColorGrades(data, min, max);
				tile.overlayLoaded = true;
				tile.overlayLoading = false;
				Platform.runLater(tileMap::update);
			} else {
				// calculate data
				// TODO: only generate data we need
				MCAFilePipe.executeParseData(new ParseDataJob(FileHelper.createRegionDirectories(tile.location), Config.getWorldUUID(), (d, u) -> {
					if (d == null) {
						System.out.println("DATA IS NULL FOR " + tile.location);
						noData.add(tile.location);
						tile.overlayLoaded = true;
						tile.overlayLoading = false;
						return;
					}
					System.out.println("DATA IS NOT NULL FOR " + tile.location);
					synchronized (Config.getWorldUUID()) {
						if (u.equals(Config.getWorldUUID())) {
							push(tile.location, d);
							tile.overlay = parseColorGrades(d, min, max);
							tile.overlayLoaded = true;
							tile.overlayLoading = false;
							Platform.runLater(tileMap::update);
						}
					}
				}));
			}
		});
	}

	private Image parseColorGrades(int[] data, int min, int max) {
		int[] colors = new int[1024];
		for (int i = 0; i < 1024; i++) {
			colors[i] = getColorGrade(data[i], min, max);
		}

		WritableImage image = new WritableImage(32, 32);
		image.getPixelWriter().setPixels(0, 0, 32, 32, PixelFormat.getIntArgbPreInstance(), colors, 0, 32);

		return image;
	}

	private static int getColorGrade(int value, int min, int max) {
		if (value <= min) {
			return 0xFF00FF00; // green
		}
		if (value >= max) {
			return 0xFFFF0000; // red
		}

		long middle = (min + max) / 2;
		double scale = 255D / (middle - min);

		if (value < middle) {
			return 0xFF00FF00 | (int) ((value - min) * scale) << 16;
		} else {
			return 0xFFFF0000 | (int) (255 - (( value - middle) * scale)) << 8;
		}
	}

	public void push(Point2i location, int[] data) {
		try {
			dataCache.setData(tileMap.getOverlayType().instance(), null, location, data);
		} catch (Exception ex) {
			Debug.dumpException("failed to cache data for region " + location, ex);
		}
	}

	public void switchTo(String dbPath) {
		try {
			dataCache.switchTo(dbPath);
		} catch (SQLException ex) {
			Debug.dumpException("failed to switch cache db", ex);
		}
	}

	public void clear() {
		try {
			dataCache.clear();
		} catch (Exception ex) {
			Debug.dumpException("failed to clear data cache", ex);
		}
		noData.clear();
	}

	public void discardData(Point2i region) {
		try {
			dataCache.deleteData(region);
			Debug.dumpf("removed data for %s from data pool", region);
		} catch (SQLException ex) {
			Debug.dumpException("failed to remove data from cache", ex);
		}
		noData.remove(region);
	}
}
