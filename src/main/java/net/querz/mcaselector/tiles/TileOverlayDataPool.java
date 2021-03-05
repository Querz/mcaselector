package net.querz.mcaselector.tiles;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.point.Point2i;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TileOverlayDataPool {

	// when key present, but value null: no cache file
	// when key not present: look if region exists
	private Map<Point2i, long[]> dataCache = new LinkedHashMap<>();

	public Image requestImage(Point2i region, long min, long max) throws IOException {
		// check if data for this region exists

		if (dataCache.containsKey(region)) {
			if (dataCache.get(region) != null) {
				// get data from cache
				int[] colors = new int[32 * 32];

				long[] data = dataCache.get(region);

				for (int i = 0; i < 1024; i++) {
					colors[i] = getColorGrade(data[i], min, max);
				}

				WritableImage image = new WritableImage(32, 32);
				image.getPixelWriter().setPixels(0, 0, 32, 32, PixelFormat.getIntArgbPreInstance(), colors, 0, 32);

				return image;

			} else {

			}
		}

		return null;
	}

	private int getOffset(Point2i region) {
		Point2i offsetPoint = region.mod(16);
		int relativeOffsetX = offsetPoint.getX() < 0 ? offsetPoint.getX() + 16 : offsetPoint.getX();
		int relativeOffsetZ = offsetPoint.getZ() < 0 ? offsetPoint.getZ() + 16 : offsetPoint.getZ();
		return (relativeOffsetX * 16 + relativeOffsetZ) * 1024;
	}

	private int getColorGrade(long value, long min, long max) {
		if (value <= min) {
			return 0xFF00FF00; // green
		}
		if (value >= max) {
			return 0xFFFF0000; // red
		}

		long middle = (min + max) / 2;
		double scale = 255D / (middle - min);

		if (value < middle) {
			return 0xFFFF0000 | (int) ((value - min) * scale) << 8;
		} else {
			return 0xFF00FF00 | (int) (255 - (( value - middle) * scale)) << 16;
		}
	}

	public void addData(Point2i region, long[] data) {
		Point2i cachePoint = region.shiftRight(4);
		int offset = getOffset(region);

		if (dataCache.containsKey(cachePoint)) {
			long[] cachedData = dataCache.get(cachePoint);
			System.arraycopy(data, 0, cachedData, offset, 1024);
		} else {

		}
	}
}
