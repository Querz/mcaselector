package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import net.querz.mcaselector.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Tile {
	public static final int SIZE = 512;
	private static final Image empty;
	private Point2i location;
	protected Image image;
	private boolean loading = false;
	private boolean loaded = false;

	static {
		WritableImage wImage = new WritableImage(SIZE, SIZE);
		PixelWriter pWriter = wImage.getPixelWriter();
		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				pWriter.setColor(x, y, Color.BLACK);
			}
		}
		empty = wImage;
	}

	public Tile(Point2i location) {
		this.location = Helper.regionToBlock(Helper.blockToRegion(location));
	}

	public Image getImage() {
		return image;
	}

	public Point2i getLocation() {
		return location;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public boolean isLoading() {
		return loading;
	}

	public void unload() {
		image.cancel();
		image = null;
		loaded = false;
	}

	public synchronized void draw(GraphicsContext ctx, float scale, Point2f offset) {
//		System.out.print("drawing " + location + " ");

		if (isLoaded() && image != null) {
			ctx.drawImage(getImage(), offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
		} else {
			ctx.drawImage(empty, offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
		}
	}

	public void loadImage() {
		if (loaded) {
			System.out.println("region already loaded");
			return;
		}
		loading = true;
		Point2i p = Helper.blockToRegion(getLocation());
		String res = String.format("out/r.%d.%d.png", p.getX(), p.getY());

		System.out.println("loading region from cache: " + res);

		InputStream resourceStream = Tile.class.getClassLoader().getResourceAsStream(res);

		if (resourceStream == null) {
			System.out.println("region " + res + " not cached, generating image...");
			File file = new File("src/main/resources/r." + p.getX() + "." + p.getY() + ".mca");
			if (!file.exists()) {
				System.out.println("region file " + res + " does not exist, skipping.");
				image = new WritableImage(SIZE, SIZE);
			} else {
				Helper.runAsync(() -> {
					MCALoader loader = new MCALoader();
					MCAFile mcaFile = loader.read(file);

					BufferedImage bufferedImage = mcaFile.createImage(new Anvil112ChunkDataProcessor(), new Anvil112ColorMapping());

					image = SwingFXUtils.toFXImage(bufferedImage, null);

					try {
						ImageIO.write(bufferedImage, "png", new File("src/main/resources/" + res));
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				});
			}

		} else {
			image = new Image(resourceStream);
		}

		loading = false;
		loaded = true;
	}
}
