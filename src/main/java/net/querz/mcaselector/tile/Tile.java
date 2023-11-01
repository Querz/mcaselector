package net.querz.mcaselector.tile;

import javafx.scene.image.Image;
import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.io.ImageHelper;

import java.io.File;

public class Tile {

	public static Color REGION_GRID_COLOR = new Color(0, 0, 0, 0.5);
	public static Color CHUNK_GRID_COLOR = new Color(0.6627451f, 0.6627451f, 0.6627451f, 0.5);
	public static Color COORDINATES_COLOR = new Color(1, 1, 1, 0.5);
	public static Color EMPTY_COLOR = new Color(0.2, 0.2, 0.2, 1);
	public static double GRID_LINE_WIDTH = 1;

	public static final int SIZE = 512;
	public static final int CHUNK_SIZE = 16;
	public static final int SIZE_IN_CHUNKS = 32;
	public static final int CHUNKS = 1024;
	public static final int PIXELS = SIZE * SIZE;

	final Point2i location;
	final long longLocation;

	Image markedChunksImage;

	Image image;
	boolean loaded = false;

	Image overlay;
	boolean overlayLoaded = false;

	public Tile(Point2i location) {
		this.location = location;
		longLocation = location.asLong();
	}

	public static int getZoomLevel(float scale) {
		return Bits.getMsb((int) scale);
	}

	public boolean isVisible(TileMap tileMap) {
		return isVisible(tileMap, 0);
	}

	// returns whether this tile is visible on screen, adding a custom radius
	// as a threshold
	// threshold is measured in tiles
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

	public long getLongLocation() {
		return longLocation;
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
			return (int) (Tile.SIZE / image.getWidth()) == zoomLevel;
		}
	}

	public int getImageZoomLevel() {
		return (int) (Tile.SIZE / image.getWidth());
	}

	public void unload(boolean overlay, boolean img) {
		if (image != null) {
			image.cancel();
			if (img) {
				image = null;
			}
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

	public void clearMarkedChunksImage() {
		markedChunksImage = null; // reset markedChunksImage
	}

	public File getMCAFile() {
		return FileHelper.createMCAFilePath(location);
	}

	public void setImage(Image image) {
		this.image = image;
	}
}
