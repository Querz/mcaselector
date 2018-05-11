package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import net.querz.mcaselector.*;
import net.querz.mcaselector.anvil112.Anvil112ChunkDataProcessor;
import net.querz.mcaselector.anvil112.Anvil112ColorMapping;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class Tile {
	public static final int SIZE = 512;
	public static final int CHUNK_SIZE = 512 / 32;

	public static final Color REGION_MARKED_COLOR = new Color(1, 0, 0, 0.5);
	public static final Color CHUNK_MARKED_COLOR = new Color(0, 0, 1, 0.5);
	public static final Color REGION_GRID_COLOR = Color.BLACK;
	public static final Color CHUNK_GRID_COLOR = Color.DARKGRAY;
	public static final double GRID_LINE_WIDTH = 0.5;

	private static final Image empty;
	private static final Color emptyColor = new Color(0.2, 0.2, 0.2, 1);
	private Point2i location;
	protected Image image;
	private boolean loading = false;
	private boolean loaded = false;
	private boolean marked = false;
	private Set<Point2i> markedChunks = new HashSet<>();

	static {
		WritableImage wImage = new WritableImage(SIZE, SIZE);
		PixelWriter pWriter = wImage.getPixelWriter();
		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				pWriter.setColor(x, y, emptyColor);
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

	public void mark(boolean marked) {
		this.marked = marked;
	}

	public boolean isMarked() {
		return marked;
	}

	public void mark(Point2i chunkBlock) {
		markedChunks.add(chunkBlock);
	}

	public void unmark(Point2i chunkBlock) {
		markedChunks.remove(chunkBlock);
	}

	public synchronized void draw(GraphicsContext ctx, float scale, Point2f offset) {
		if (isLoaded() && image != null) {
			ctx.drawImage(getImage(), offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
			if (marked) {
				ctx.setFill(REGION_MARKED_COLOR);
				ctx.fillRect(offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
			} else if (markedChunks.size() > 0) {
				ctx.setFill(CHUNK_MARKED_COLOR);
				for (Point2i p : markedChunks) {
					//offset is the offset in blocks the region is drawn based on 0|0 of the canvas
					int regionChunkOffsetX = (int) ((p.getX() - location.getX() + offset.getX()) / scale);
					int regionChunkOffsetY = (int) ((p.getY() - location.getY() + offset.getY()) / scale);
					ctx.fillRect(regionChunkOffsetX, regionChunkOffsetY, CHUNK_SIZE / scale, CHUNK_SIZE / scale);
				}
			}
		} else {
			ctx.drawImage(empty, offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
		}

		ctx.setLineWidth(GRID_LINE_WIDTH);
		//draw region grid
		ctx.setStroke(REGION_GRID_COLOR);
		ctx.strokeLine(offset.getX(), offset.getY(), offset.getX(), offset.getY() + SIZE / scale);
		ctx.strokeLine(offset.getX(), offset.getY(), offset.getX() + SIZE / scale, offset.getY());
		if (scale <= TileMap.CHUNK_GRID_SCALE) {
			//draw chunk grid
			ctx.setStroke(CHUNK_GRID_COLOR);
			for (int x = 1; x < SIZE / CHUNK_SIZE; x++) {
				ctx.strokeLine(offset.getX() + (x * CHUNK_SIZE / scale), offset.getY(), offset.getX() + (x * CHUNK_SIZE / scale), offset.getY() + SIZE / scale);
			}
			for (int z = 1; z < SIZE / CHUNK_SIZE; z++) {
				ctx.strokeLine(offset.getX(), offset.getY() + (z * CHUNK_SIZE / scale), offset.getX() + SIZE / scale, offset.getY() + (z * CHUNK_SIZE / scale));
			}
		}
	}

	private void drawGridElement(GraphicsContext ctx, float x, float z, float size) {
		ctx.setStroke(Color.BLACK);
		ctx.strokeLine(x, z, x, z + size);
		ctx.strokeLine(x, z, x + size, z);
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
				image = null;
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
