package net.querz.mcaselector.io;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.util.progress.Timer;
import net.querz.mcaselector.tile.Tile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.imageio.ImageIO;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.ImageLineInt;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ImageHelper {

	private static final Logger LOGGER = LogManager.getLogger(ImageHelper.class);

	private ImageHelper() {}

	public static BufferedImage scaleImage(BufferedImage before, double newSize, boolean smooth) {
		double w = before.getWidth();
		double h = before.getHeight();
		BufferedImage after = new BufferedImage((int) newSize, (int) newSize, BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		at.scale(newSize / w, newSize / h);
		AffineTransformOp scaleOp = new AffineTransformOp(at, smooth ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		return scaleOp.filter(before, after);
	}

	public static Image scaleDownFXImage(Image before, int newSize) {
		WritableImage after = new WritableImage(newSize, newSize);
		PixelReader reader = before.getPixelReader();
		PixelWriter writer = after.getPixelWriter();

		int scaleFactor = (int) (before.getWidth() / after.getWidth());

		for (int y = 0; y < newSize; y++) {
			for (int x = 0; x < newSize; x++) {
				int argb = reader.getArgb(x * scaleFactor, y * scaleFactor);
				writer.setArgb(x, y, argb);
			}
		}

		return after;
	}

	public static Image renderGradient(int width, float min, float max, float low, float high, boolean inverted) {
		WritableImage image = new WritableImage(width, 1);
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

			pixelWriter.setArgb(i, 0, Color.HSBtoRGB(hue, saturation, brightness));
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
        progressChannel.setMax(height + 1); // +1 for the writing of the file
        progressChannel.updateProgress("", 0);

        Timer t = new Timer();

        // Write PNG using pngj with low compression for speed
        ImageInfo info = new ImageInfo(width, height, 8, true);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PngWriter writer = new PngWriter(fos, info);
            writer.setCompLevel(1);
            for (int y = 0; y < height && !progressChannel.taskCancelled(); y++) {
                int start = y * width;
                ImageLineInt line = new ImageLineInt(info);
                int[] scanline = line.getScanline();
                for (int x = 0; x < width; x++) {
                    int argb = data[start + x];
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = (argb) & 0xFF;
                    int a = (argb >>> 24) & 0xFF;
                    int idx = x * 4;
                    scanline[idx] = r;
                    scanline[idx + 1] = g;
                    scanline[idx + 2] = b;
                    scanline[idx + 3] = a;
                }
                writer.writeRow(line);
                progressChannel.incrementProgress("");
            }
            writer.end();
        }

        progressChannel.incrementProgress("");

        progressChannel.done("done");
        LOGGER.debug("took {} to save image {}", t, file);
    }
}
