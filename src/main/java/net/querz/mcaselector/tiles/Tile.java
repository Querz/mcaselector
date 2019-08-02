package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.io.MCAFile;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2f;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Timer;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Tile {

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

	private Image markedChunksImage;

	private boolean loading = false;
	private boolean loaded = false;
	private boolean marked = false;
	//a set of all marked chunks in the tile in block locations
	private Set<Point2i> markedChunks = new HashSet<>();

	static {
		reloadEmpty();
	}

	public Tile(Point2i location) {
		this.location = location;
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
		return location.getX() * SIZE >= min.getX() && location.getY() * SIZE >= min.getY()
				&& location.getX() * SIZE <= max.getX() && location.getY() * SIZE <= max.getY();
	}

	public Image getImage() {
		return image;
	}

	public Point2i getLocation() {
		return location;
	}

	public boolean isEmpty() {
		return image == null || image == empty;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
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
		if (markedChunksImage != null) {
			markedChunksImage.cancel();
			markedChunksImage = null;
		}
		loaded = false;
	}

	public void mark(boolean marked) {
		this.marked = marked;
		if (marked) {
			markedChunks.clear();
			markedChunksImage = null;
		}
	}

	public void mark(Point2i chunkBlock) {
		markedChunks.add(chunkBlock);
		if (markedChunks.size() == CHUNKS) {
			mark(true);
		} else {
			markedChunksImage = null; //reset markedChunksImage
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
			Point2i regionChunk = Helper.regionToChunk(location);
			for (int x = 0; x < SIZE_IN_CHUNKS; x++) {
				for (int z = 0; z < SIZE_IN_CHUNKS; z++) {
					markedChunks.add(regionChunk.add(x, z));
				}
			}
			mark(false);
		}
		markedChunks.remove(chunkBlock);
		markedChunksImage = null; //reset markedChunksImage
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

	public void draw(GraphicsContext ctx, float scale, Point2f offset) {
		if (isLoaded() && image != null) {
			double size =  getImage().getHeight() * (SIZE / getImage().getWidth());
			ctx.drawImage(getImage(), offset.getX(), offset.getY(), size / scale, size / scale);
			if (marked) {
				//draw marked region
				ctx.setFill(Config.getRegionSelectionColor());
				ctx.fillRect(offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
			} else if (markedChunks.size() > 0) {

				if (markedChunksImage == null) {
					createMarkedChunksImage(Helper.getZoomLevel(scale));
				}

				// apply markedChunksImage to ctx

				ctx.drawImage(markedChunksImage, offset.getX(), offset.getY(), size / scale, size / scale);
			}
		} else {
			ctx.drawImage(empty, offset.getX(), offset.getY(), SIZE / scale, SIZE / scale);
		}
	}

	void createMarkedChunksImage(int zoomLevel) {
		WritableImage wImage = new WritableImage(SIZE / zoomLevel, SIZE / zoomLevel);

		Canvas canvas = new Canvas(SIZE / (float) zoomLevel, SIZE / (float) zoomLevel);
		GraphicsContext ctx = canvas.getGraphicsContext2D();
		ctx.setFill(Config.getChunkSelectionColor());

		for (Point2i markedChunk : markedChunks) {
			Point2i regionChunk = markedChunk.mod(SIZE_IN_CHUNKS);
			if (regionChunk.getX() < 0) {
				regionChunk.setX(regionChunk.getX() + SIZE_IN_CHUNKS);
			}
			if (regionChunk.getY() < 0) {
				regionChunk.setY(regionChunk.getY() + SIZE_IN_CHUNKS);
			}

			ctx.fillRect(regionChunk.getX() * CHUNK_SIZE / (float) zoomLevel, regionChunk.getY() * CHUNK_SIZE / (float) zoomLevel, CHUNK_SIZE / (float) zoomLevel, CHUNK_SIZE / (float) zoomLevel);
		}

		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT);

		canvas.snapshot(params, wImage);

		markedChunksImage = wImage;
	}

	public File getMCAFile() {
		return Helper.createMCAFilePath(location);
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public void mergeImage(BufferedImage image, Point2i location) {
		BufferedImage bufferedImage = SwingFXUtils.fromFXImage(this.image, null);
		Helper.mergeImages(image, bufferedImage, location);
		this.image = SwingFXUtils.toFXImage(bufferedImage, null);
	}

	public void loadFromCache(Runnable callback, Supplier<Float> scaleSupplier) {
		if (loaded) {
			Debug.dump("region at " + location + " already loaded");
			return;
		}

		if (Config.getCacheDir() == null) {
			//load empty map (start screen)
			loaded = true;
			callback.run();
			return;
		}

		String res = String.format(Config.getCacheDir().getAbsolutePath() + "/" + Helper.getZoomLevel(scaleSupplier.get()) + "/r.%d.%d.png", location.getX(), location.getY());

		Debug.dump("loading region " + location + " from cache: " + res);

		try (InputStream inputStream = new FileInputStream(res)) {
			image = new Image(inputStream);
			loaded = true;
			callback.run();
		} catch (IOException ex) {
			Debug.dump("region " + location + " not cached");
		}
	}

	public Image generateImage(Runnable callback, byte[] rawData) {
		if (loaded) {
			Debug.dump("region at " + location + " already loaded");
			return image;
		}

		Timer t = new Timer();

		File file = getMCAFile();

		ByteArrayPointer ptr = new ByteArrayPointer(rawData);

		MCAFile mcaFile = MCAFile.readHeader(file, ptr);
		if (mcaFile == null) {
			Debug.error("error reading mca file " + file);
			//mark as loaded, we won't try to load this again
			loaded = true;
			return image;
		}
		Debug.dumpf("took %s to read mca file header of %s", t, file.getName());

		t.reset();

		image = mcaFile.createImage(ptr);
		loaded = true;

		callback.run();

		Debug.dumpf("took %s to generate image of %s", t, file.getName());

		return image;
	}
}
