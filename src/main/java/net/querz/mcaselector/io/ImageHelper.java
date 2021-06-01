package net.querz.mcaselector.io;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.tiles.Tile;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public final class ImageHelper {

	private ImageHelper() {}

	public static BufferedImage scaleImage(BufferedImage before, double newSize) {
		double w = before.getWidth();
		double h = before.getHeight();
		BufferedImage after = new BufferedImage((int) newSize, (int) newSize, BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		at.scale(newSize / w, newSize / h);
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		return scaleOp.filter(before, after);
	}

	public static Image renderGradient(int width, float min, float max, float low, float high, boolean inverted) {
		WritableImage image = new WritableImage(width, 50);
		PixelWriter pixelWriter = image.getPixelWriter();
		for (int i = 0; i < width; i++) {
			float hue = (max - min) * ((float) i / width) + min;
			float saturation = 1, brightness = 1;

			if (hue < low || hue > high) {
				saturation = 0.3f;
				brightness = 0.6f;
			}
			if (inverted) {
				hue = max - hue + min;
			}

			for (int j = 0; j < 50; j++) {
				pixelWriter.setArgb(i, j, Color.HSBtoRGB(hue, saturation, brightness));
			}
		}
		return image;
	}

	public static float getHue(int color) {
		return getHue(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF);
	}

	public static float getHue(float r, float g, float b) {
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

	private static Image empty;

	public static void reloadEmpty() {
		WritableImage wImage = new WritableImage(Tile.SIZE, Tile.SIZE);
		PixelWriter pWriter = wImage.getPixelWriter();
		for (int x = 0; x < Tile.SIZE; x++) {
			for (int y = 0; y < Tile.SIZE; y++) {
				pWriter.setColor(x, y, Tile.EMPTY_COLOR.makeJavaFXColor());
			}
		}
		empty = wImage;
	}

	public static Image getEmptyTileImage() {
		return empty;
	}

	public static void saveImageData(int[] data, int width, int height, File file, Progress progressChannel) throws IOException {
		progressChannel.setMax(height);
		progressChannel.updateProgress("", 0);

		Timer t = new Timer();

		ImageInfo imi = new ImageInfo(width, height, 8, true); // 8 bits per channel, alpha

		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file), 65536)) {
			PngWriter png = new PngWriter(bos, imi);
			png.setFilterType(FilterType.FILTER_ADAPTIVE_FAST);

			for (int row = 0; row < png.imgInfo.rows && !progressChannel.taskCancelled(); row++) {
				ImageLineInt iline = new ImageLineInt(imi);
				int[] copy = Arrays.copyOfRange(data, row * width, row * width + width);
				ImageLineHelper.setPixelsRGBA8(iline, copy);
				png.writeRow(iline);

				progressChannel.incrementProgress("");
			}

			png.end();

		} finally {
			progressChannel.done("done");
			Debug.dumpf("took %s to save image %s", t, file);
		}
	}
}
