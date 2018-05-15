package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.text.Font;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.anvil112.Anvil112ChunkDataProcessor;
import net.querz.mcaselector.anvil112.Anvil112ColorMapping;
import net.querz.mcaselector.io.MCAFile;
import net.querz.mcaselector.io.MCALoader;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2f;
import net.querz.mcaselector.util.Point2i;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Tile {
	public static final int SIZE = 512;
	public static final int CHUNK_SIZE = 16;

	public static final Color REGION_MARKED_COLOR = new Color(1, 0, 0, 0.5);
	public static final Color CHUNK_MARKED_COLOR = new Color(0, 0, 1, 0.5);
	public static final Color REGION_GRID_COLOR = Color.BLACK;
	public static final Color CHUNK_GRID_COLOR = Color.DARKGRAY;
	public static final double GRID_LINE_WIDTH = 0.5;

	private static final Image empty;
	private static final Color emptyColor = new Color(0.2, 0.2, 0.2, 1);
	private Point2i location;
	private Image image;
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

	public boolean isEmpty() {
		return isLoaded() && (image == null || image == empty);
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
		if (marked) {
			markedChunks.clear();
		}
	}

	public boolean isMarked() {
		return marked;
	}

	public void mark(Point2i chunkBlock) {
		markedChunks.add(chunkBlock);
		if (markedChunks.size() == 1024) {
			markedChunks.clear();
			mark(true);
		}
	}

	public void unmark(Point2i chunkBlock) {
		if (isMarked()) {
			for (int x = 0; x < 32; x++) {
				for (int z = 0; z < 32; z++) {
					markedChunks.add(new Point2i(location.getX() + x * CHUNK_SIZE, location.getY() + z * CHUNK_SIZE));
				}
			}
			mark(false);
		}
		markedChunks.remove(chunkBlock);
	}

	public void clearMarks() {
		mark(false);
		markedChunks.clear();
	}

	public boolean isMarked(Point2i chunkBlock) {
		return isMarked() || markedChunks.contains(chunkBlock);
	}

	public Set<Point2i> getMarkedChunks() {
		return markedChunks;
	}

	public synchronized void draw(GraphicsContext ctx, float scale, Point2f offset, boolean regionGrid, boolean chunkGrid) {
		if (isLoaded() && image != null) {
			ctx.drawImage(getImage(), offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
			if (marked) {
				ctx.setFill(REGION_MARKED_COLOR);
				ctx.fillRect(offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
			} else if (markedChunks.size() > 0) {
				ctx.setFill(CHUNK_MARKED_COLOR);
				for (Point2i p : markedChunks) {
					//location is the real location of the region in the world
					//p is the real location of the chunk in the world
					//offset is the offset in pixel the region is drawn based on 0|0 of the canvas
					//need the location of the chunk inside the region in pixel
					//p.x - location.x --> location in blocks
					//(p.x - location.x) / scale --> location in pixel
					int regionChunkOffsetX = (int) ((p.getX() - location.getX()) / scale + offset.getX());
					int regionChunkOffsetY = (int) ((p.getY() - location.getY()) / scale + offset.getY());
					ctx.fillRect(regionChunkOffsetX, regionChunkOffsetY, CHUNK_SIZE / scale, CHUNK_SIZE / scale);
				}
			}
		} else {
			ctx.drawImage(empty, offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
		}

		if (regionGrid) {
			ctx.setLineWidth(GRID_LINE_WIDTH);
			//draw region grid
			ctx.setStroke(REGION_GRID_COLOR);
			ctx.strokeLine(offset.getX(), offset.getY(), offset.getX(), offset.getY() + SIZE / scale);
			ctx.strokeLine(offset.getX(), offset.getY(), offset.getX() + SIZE / scale, offset.getY());

		}

		if (chunkGrid && scale <= TileMap.CHUNK_GRID_SCALE) {
			ctx.setLineWidth(GRID_LINE_WIDTH);
			//draw chunk grid
			ctx.setStroke(CHUNK_GRID_COLOR);
			for (int x = regionGrid ? 1 : 0; x < SIZE / CHUNK_SIZE; x++) {
				ctx.strokeLine(offset.getX() + (x * CHUNK_SIZE / scale), offset.getY(), offset.getX() + (x * CHUNK_SIZE / scale), offset.getY() + SIZE / scale);
			}
			for (int z = regionGrid ? 1 : 0; z < SIZE / CHUNK_SIZE; z++) {
				ctx.strokeLine(offset.getX(), offset.getY() + (z * CHUNK_SIZE / scale), offset.getX() + SIZE / scale, offset.getY() + (z * CHUNK_SIZE / scale));
			}
		}
	}

	public void loadImage(TileMap tileMap) {
		if (loaded) {
			System.out.println("region already loaded");
			return;
		}
		loading = true;
		Point2i p = Helper.blockToRegion(getLocation());
		String res = String.format(Config.getCacheDir().getAbsolutePath() + "/r.%d.%d.png", p.getX(), p.getY());

		System.out.println("loading region from cache: " + res);

		InputStream resourceStream = null;
		try {
			resourceStream = new FileInputStream(res);
		} catch (FileNotFoundException e) {
			//ignore, we print a warning later
		}

//		InputStream resourceStream = Tile.class.getClassLoader().getResourceAsStream(res);

		if (resourceStream == null) {
			System.out.println("region " + res + " not cached, generating image...");
			File file = new File(Config.getWorldDir().getAbsolutePath() + "/r." + p.getX() + "." + p.getY() + ".mca");
			if (!file.exists()) {
				System.out.println("region file " + res + " does not exist, skipping.");
				image = null;
			} else {
				Helper.runAsync(() -> {

					try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

						MCAFile mcaFile = MCALoader.read(file, raf);

						BufferedImage bufferedImage = mcaFile.createImage(new Anvil112ChunkDataProcessor(), new Anvil112ColorMapping(), raf);

						image = SwingFXUtils.toFXImage(bufferedImage, null);

						File cacheFile = new File(res);
						if (!cacheFile.getParentFile().exists()) {
							cacheFile.getParentFile().mkdirs();
						}

						tileMap.update();

						ImageIO.write(bufferedImage, "png", new File(res));

					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

		} else {
			image = new Image(resourceStream);
			try {
				resourceStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		loading = false;
		loaded = true;
	}
}
