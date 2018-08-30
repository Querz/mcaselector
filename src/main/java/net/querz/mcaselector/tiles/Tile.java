package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.io.MCAFile;
import net.querz.mcaselector.io.MCALoader;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2f;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Timer;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Tile {

	public static Color REGION_MARKED_COLOR = new Color(1, 0, 0, 0.8);
	public static Color CHUNK_MARKED_COLOR = new Color(1, 0.45, 0, 0.8);
	public static Color REGION_GRID_COLOR = Color.BLACK;
	public static Color CHUNK_GRID_COLOR = Color.DARKGRAY;
	public static Color EMPTY_CHUNK_BACKGROUND_COLOR = Color.BLACK;
	public static Color EMPTY_COLOR = new Color(0.2, 0.2, 0.2, 1);
	public static double GRID_LINE_WIDTH = 0.5;

	public static final int SIZE = 512;
	public static final int CHUNK_SIZE = 16;
	public static final int SIZE_IN_CHUNKS = 32;
	public static final int CHUNKS = 1024;

	private static Image empty;

	private Point2i location;
	private Image image;
	private boolean loading = false;
	private boolean loaded = false;
	private boolean marked = false;
	private Set<Point2i> markedChunks = new HashSet<>();

	static {
		reloadEmpty();
	}

	public Tile(Point2i location) {
		this.location = Helper.regionToBlock(Helper.blockToRegion(location));
	}

	public boolean isVisible(TileMap tileMap) {
		return isVisible(tileMap, 0);
	}

	//returns whether this tile is visible on screen, adding a custom radius
	//as a threshold
	//threshold is measured in tiles
	public boolean isVisible(TileMap tileMap, int threshold) {
		Point2i o = tileMap.getOffset().toPoint2i();
		Point2i min = Helper.regionToBlock(Helper.blockToRegion(o.sub(threshold * SIZE)));
		Point2i max = Helper.regionToBlock(Helper.blockToRegion(new Point2i(
				(int) (o.getX() + tileMap.getWidth() * tileMap.getScale()),
				(int) (o.getY() + tileMap.getHeight() * tileMap.getScale())).add(threshold * SIZE)));
		return location.getX() >= min.getX() && location.getY() >= min.getY()
				&& location.getX() <= max.getX() && location.getY() <= max.getY();
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

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public boolean isLoading() {
		return loading;
	}

	public void unload() {
		if (image != null) {
			image.cancel();
			image = null;
		}
		loaded = false;
	}

	public void mark(boolean marked) {
		this.marked = marked;
		if (marked) {
			markedChunks.clear();
		}
	}

	public void mark(Point2i chunkBlock) {
		markedChunks.add(chunkBlock);
		if (markedChunks.size() == CHUNKS) {
			markedChunks.clear();
			mark(true);
		}
	}

	public boolean isMarked() {
		return marked;
	}

	public boolean isMarked(Point2i chunkBlock) {
		return isMarked() || markedChunks.contains(chunkBlock);
	}

	public void unMark(Point2i chunkBlock) {
		if (isMarked()) {
			for (int x = 0; x < SIZE_IN_CHUNKS; x++) {
				for (int z = 0; z < SIZE_IN_CHUNKS; z++) {
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

	public Set<Point2i> getMarkedChunks() {
		return markedChunks;
	}

	public static void reloadEmpty() {
		WritableImage wImage = new WritableImage(SIZE, SIZE);
		PixelWriter pWriter = wImage.getPixelWriter();
		for (int x = 0; x < SIZE; x++) {
			for (int y = 0; y < SIZE; y++) {
				pWriter.setColor(x, y, EMPTY_COLOR);
			}
		}
		empty = wImage;
	}

	public void draw(GraphicsContext ctx, float scale, Point2f offset, boolean regionGrid, boolean chunkGrid) {
		if (isLoaded() && image != null) {
			ctx.drawImage(getImage(), offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
			if (marked) {
				//draw marked region
				ctx.setFill(REGION_MARKED_COLOR);
				ctx.fillRect(offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
			} else if (markedChunks.size() > 0) {
				//draw marked chunks
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
					ctx.fillRect(
							regionChunkOffsetX,
							regionChunkOffsetY,
							Math.ceil(CHUNK_SIZE / scale),
							Math.ceil(CHUNK_SIZE / scale));
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
				ctx.strokeLine(
						offset.getX() + (x * CHUNK_SIZE / scale),
						offset.getY(),
						offset.getX() + (x * CHUNK_SIZE / scale),
						offset.getY() + SIZE / scale);
			}
			for (int z = regionGrid ? 1 : 0; z < SIZE / CHUNK_SIZE; z++) {
				ctx.strokeLine(
						offset.getX(),
						offset.getY() + (z * CHUNK_SIZE / scale),
						offset.getX() + SIZE / scale,
						offset.getY() + (z * CHUNK_SIZE / scale));
			}
		}
	}

	public boolean isCached() {
		return Config.getCacheDir() == null || Helper.createPNGFilePath(Helper.blockToRegion(location)).exists();
	}

	public File getMCAFile() {
		return Helper.createMCAFilePath(Helper.blockToRegion(location));
	}

	public File getCacheFile() {
		return Helper.createPNGFilePath(Helper.blockToRegion(location));
	}

	public void loadFromCache(TileMap tileMap) {
		if (loaded) {
			Debug.dump("region at " + location + " already loaded");
			return;
		}
		loading = true;
		Point2i p = Helper.blockToRegion(location);

		if (Config.getCacheDir() == null) {
			//load empty map (start screen)
			loading = false;
			loaded = true;
			return;
		}

		String res = String.format(Config.getCacheDir().getAbsolutePath() + "/r.%d.%d.png", p.getX(), p.getY());

		Debug.dump("loading region " + p + " from cache: " + res);

		try (InputStream inputStream = new FileInputStream(res)) {
			image = new Image(inputStream);
			loaded = true;
		} catch (IOException ex) {
			Debug.dump("region " + p + " not cached");
			//do nothing
		}
		loading = false;
		Platform.runLater(tileMap::update);
	}

	public Image generateImage(TileMap tileMap, byte[] rawData) {
		if (loaded) {
			Debug.dump("region at " + location + " already loaded");
			return image;
		}

		Timer t = new Timer();

		loading = true;
		File file = getMCAFile();

		ByteArrayPointer ptr = new ByteArrayPointer(rawData);

		MCAFile mcaFile = MCAFile.read(file, ptr);
		if (mcaFile == null) {
			Debug.error("error reading mca file " + file);
			//mark as loaded, we won't try to load this again
			loading = false;
			loaded = true;
			return image;
		}
		Debug.dumpf("took %s to read mca file header of %s", t, file.getName());

		t.reset();

		image = mcaFile.createImage(ptr);
		loading = false;
		loaded = true;

		Platform.runLater(tileMap::update);

		Debug.dumpf("took %s to generate image of %s", t, file.getName());

		return image;
	}
}
