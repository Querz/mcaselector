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
import java.awt.*;
import java.sql.SQLException;
import java.util.Collections;
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

	public void setParser(OverlayParser overlay) {
		this.parser = overlay;
		if (overlay != null) {
			try {
				dataCache.initTables(Collections.singletonList(overlay));
			} catch (SQLException ex) {
				Debug.dumpException("failed to create table for overlay " + overlay, ex);
			}
		}
	}

	public void requestImage(Tile tile, OverlayParser parser) {
		// check if data for this region exists
		if (noData.contains(tile.location)) {
			return;
		}

		if (ParseDataJob.isLoading(tile)) {
			// skip if we are already loading this tile
			return;
		}

		if (parser == null) {
			return;
		}

		ParseDataJob.setLoading(tile, true);

		overlayCacheLoaders.execute(() -> {
			int[] data = null;
			try {
				data = dataCache.getData(parser, null, tile.location);
			} catch (Exception ex) {
				Debug.dumpException("failed to load cached overlay data for region " + tile.location, ex);
			}

			if (data != null) {
				tile.overlay = parseColorGrades(data, parser.min(), parser.max());
				tile.overlayLoaded = true;
				ParseDataJob.setLoading(tile, false);
				Platform.runLater(tileMap::update);
			} else {
				// calculate data
				MCAFilePipe.executeParseData(new ParseDataJob(tile, FileHelper.createRegionDirectories(tile.location), Config.getWorldUUID(), (d, u) -> {
					Platform.runLater(() -> {
						if (u.equals(Config.getWorldUUID())) {
							if (d == null) {
								noData.add(tile.location);
								tile.overlayLoaded = true;
								return;
							}
							if (parser.equals(this.parser)) {
								push(tile.location, d);
								tile.overlay = parseColorGrades(d, parser.min(), parser.max());
								tile.overlayLoaded = true;
								tileMap.update();
							}
						}
					});
				}, parser));
			}
		});
	}

	private Image parseColorGrades(int[] data, int min, int max) {
		int[] colors = new int[1024];
		for (int i = 0; i < 1024; i++) {
			colors[i] = getColorGrade(data[i], min, max, 0xFF0000FF, 0xFFFF0000);
		}

		WritableImage image = new WritableImage(32, 32);
		image.getPixelWriter().setPixels(0, 0, 32, 32, PixelFormat.getIntArgbPreInstance(), colors, 0, 32);

		return image;
	}

	private static int getColorGrade(int value, int min, int max, int minColor, int maxColor) {
		if (value <= min) {
			return minColor;
		}
		if (value >= max) {
			return maxColor;
		}

		float hue1 = getHue(minColor >> 16 & 0xFF, minColor >> 8 & 0xFF, minColor & 0xFF);
		float hue2 = getHue(maxColor >> 16 & 0xFF, maxColor >> 8 & 0xFF, maxColor & 0xFF);

		float percent = (float) (value - min) / (max - min);
		float hue = hue1 + percent * (hue2 - hue1);

		return Color.HSBtoRGB(hue, 1, 1);
	}

	private static float getHue(float r, float g, float b) {
		float min = Math.min(Math.min(r, g), b);
		float max = Math.max(Math.max(r, g), b);

		if (min == max) {
			return 0f;
		}

		float hue;
		if (r == max) {
			hue = (g - b) / (max - min);
		} else if (g == max) {
			hue = 2f + (b - r) / (max - min);
		} else {
			hue = 4f + (r - g) / (max - min);
		}

		hue *= 60f;
		if (hue < 0f) {
			hue += 360f;
		}

		return hue / 360f;
	}

	public void push(Point2i location, int[] data) {
		try {
			dataCache.setData(tileMap.getOverlay(), null, location, data);
		} catch (Exception ex) {
			Debug.dumpException("failed to cache data for region " + location, ex);
		}
	}

	public void switchTo(String dbPath) {
		try {
			dataCache.switchTo(dbPath, tileMap.getOverlayParsers());
		} catch (SQLException ex) {
			Debug.dumpException("failed to switch cache db", ex);
		}
	}

	public void clear() {
		try {
			dataCache.clear(tileMap.getOverlayParsers());
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
