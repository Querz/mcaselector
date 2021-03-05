package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.overlay.OverlayDataParser;
import net.querz.mcaselector.tiles.overlay.OverlayType;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class OverlayDataPool {

	private final TileMap tileMap;

	// when key present, but value null: no cache file
	// when key not present: look if region exists
	private final Map<Point2i, long[]> dataCache = new LinkedHashMap<>();
	private final double poolSize;
	private OverlayDataParser parser;

	public OverlayDataPool(TileMap tileMap, double poolSize) {
		this.tileMap = tileMap;
		this.poolSize = poolSize;
	}

	public void setType(OverlayType type) {
		this.parser = type == null ? null : type.instance();
	}

	public void requestImage(Tile tile, Point2i region, long min, long max) {
		// check if data for this region exists

		if (parser == null) {
			return;
		}

		tile.overlayLoading = true;

		if (dataCache.containsKey(region)) {
			if (dataCache.get(region) != null) {
				// get data from cache

				long[] data = dataCache.get(region);

				tile.overlay = parseColorGrades(data, min, max);
				Platform.runLater(tileMap::update);
			}
			// else: no data
		} else {
			// try to get data from disk
			// look if we already have the cache file
			File cacheFile = new File(Config.getCacheDir(), parser.name() + "/" + FileHelper.createDATFileName(tile.getLocation()));
			if (cacheFile.exists()) {
				// load cache file
				long[] data = new long[1024];
				try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(cacheFile)), 8192))) {
					for (int i = 0; i < 1024; i++) {
						data[i] = dis.readLong();
					}
					dataCache.put(region, data);
					tile.overlay = parseColorGrades(data, min, max);
					Platform.runLater(tileMap::update);
				} catch (IOException ex) {
					Debug.dumpException("failed to load data cache file " + cacheFile, ex);
				}
			}
			// else: do nothing
		}
		tile.overlayLoaded = true;
		tile.overlayLoading = false;
	}

	private Image parseColorGrades(long[] data, long min, long max) {
		int[] colors = new int[1024];
		for (int i = 0; i < 1024; i++) {
			colors[i] = getColorGrade(data[i], min, max);
		}

		WritableImage image = new WritableImage(32, 32);
		image.getPixelWriter().setPixels(0, 0, 32, 32, PixelFormat.getIntArgbPreInstance(), colors, 0, 32);

		return image;
	}

	private static int getColorGrade(long value, long min, long max) {
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

	public void push(Point2i location, long[] data) {
		dataCache.put(location, data);
		trim();
	}

	private void trim() {
		if (dataCache.size() <= tileMap.getVisibleTiles() * poolSize) {
			return;
		}
		Iterator<Point2i> it = dataCache.keySet().iterator();
		while (it.hasNext() && dataCache.size() > tileMap.getVisibleTiles() * poolSize) {
			Point2i removed = it.next();
			it.remove();
			Debug.dumpf("removed %s from data pool", removed);
		}
	}

	public void clear() {
		dataCache.clear();
	}
}
