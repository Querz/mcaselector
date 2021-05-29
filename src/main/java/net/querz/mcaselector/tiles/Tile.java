package net.querz.mcaselector.tiles;

import javafx.scene.image.Image;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.io.ImageHelper;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Tile {

	public static Color REGION_GRID_COLOR = new Color(0, 0, 0, 0.5);
	public static Color CHUNK_GRID_COLOR = new Color(0.6627451f, 0.6627451f, 0.6627451f, 0.5);
	public static Color EMPTY_COLOR = new Color(0.2, 0.2, 0.2, 1);
	public static double GRID_LINE_WIDTH = 1;

	public static final int SIZE = 512;
	public static final int CHUNK_SIZE = 16;
	public static final int SIZE_IN_CHUNKS = 32;
	public static final int CHUNKS = 1024;
	public static final int PIXELS = SIZE * SIZE;

	final Point2i location;

	Image markedChunksImage;

	Image image;
	boolean loaded = false;

	boolean marked = false;
	// a set of all marked chunks in the tile in chunk locations
	Set<Point2i> markedChunks = new HashSet<>();

	Image overlay;
	boolean overlayLoaded = false;

	public Tile(Point2i location) {
		this.location = location;
	}

	public static int getZoomLevel(float scale) {
		int b = 1;
		while (b <= scale) {
			b = b << 1;
		}
		return (int) Math.ceil(b / 2.0);
	}

	public boolean isVisible(TileMap tileMap) {
		return isVisible(tileMap, 0);
	}

	//returns whether this tile is visible on screen, adding a custom radius
	//as a threshold
	//threshold is measured in tiles
	public boolean isVisible(TileMap tileMap, int threshold) {
		Point2i o = tileMap.getOffset().toPoint2i();
		Point2i min = o.sub(threshold * SIZE).blockToRegion().regionToBlock();
		Point2i max = new Point2i(
				(int) (o.getX() + tileMap.getWidth() * tileMap.getScale()),
				(int) (o.getZ() + tileMap.getHeight() * tileMap.getScale())).add(threshold * SIZE).blockToRegion().regionToBlock();
		return location.getX() * SIZE >= min.getX() && location.getZ() * SIZE >= min.getZ()
				&& location.getX() * SIZE <= max.getX() && location.getZ() * SIZE <= max.getZ();
	}

	public Image getImage() {
		return image;
	}

	public Image getOverlay() {
		return overlay;
	}

	public boolean isOverlayLoaded() {
		return overlayLoaded;
	}

	public Point2i getLocation() {
		return location;
	}

	public boolean isEmpty() {
		return image == null || image == ImageHelper.getEmptyTileImage();
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public boolean matchesZoomLevel(int zoomLevel) {
		if (image == null) {
			return true;
		} else {
			return (int) (Tile.SIZE * (1D / image.getWidth())) == zoomLevel;
		}
	}

	public void unload(boolean overlay) {
		if (image != null) {
			image.cancel();
		}
		if (markedChunksImage != null) {
			markedChunksImage.cancel();
			markedChunksImage = null;
		}
		if (overlay && this.overlay != null) {
			this.overlay.cancel();
			this.overlay = null;
		}
		loaded = false;
	}

	public void mark(boolean marked) {
		this.marked = marked;
		markedChunks = new HashSet<>();
		markedChunksImage = null;
	}

	public void mark(Point2i chunk) {
		// don't do anything if the entire tile is already marked
		if (isMarked()) {
			return;
		}
		int sizeBefore = markedChunks.size();
		markedChunks.add(chunk);
		if (markedChunks.size() == CHUNKS) {
			mark(true);
		} else if (sizeBefore != markedChunks.size()) {
			markedChunksImage = null; // reset markedChunksImage if there was a change
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
			mark(false);
			Point2i regionChunk = location.regionToChunk();
			for (int x = 0; x < SIZE_IN_CHUNKS; x++) {
				for (int z = 0; z < SIZE_IN_CHUNKS; z++) {
					markedChunks.add(regionChunk.add(x, z));
				}
			}
		}
		markedChunks.remove(chunkBlock);
		markedChunksImage = null; //reset markedChunksImage
	}

	public void clearMarks() {
		mark(false);
		markedChunks = new HashSet<>();
	}

	public Set<Point2i> getMarkedChunks() {
		return markedChunks;
	}

	public File getMCAFile() {
		return FileHelper.createMCAFilePath(location);
	}

	public void setImage(Image image) {
		this.image = image;
	}
}
