package net.querz.mcaselector.tiles;

import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import net.querz.mcaselector.Helper;
import net.querz.mcaselector.Point2f;
import net.querz.mcaselector.Point2i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TileMap extends Group {
	private float scale = 1;	//higher --> +
								//lower -->  -

	public static final float MAX_SCALE = 5;
	public static final float MIN_SCALE = 0.2f;

	private int width, height;

	private Canvas canvas;
	private GraphicsContext context;

	private Point2f offset = new Point2f();

	private Point2f previousMouseLocation = null;
	private Point2f firstMouseLocation = null;

	private Map<Point2i, Tile> tiles = new HashMap<>();

	public TileMap(int width, int height) {
		this.width = width;
		this.height = height;
		canvas = new Canvas(width, height);

		context = canvas.getGraphicsContext2D();

		this.getChildren().add(canvas);

		this.setOnMousePressed(this::onMousePressed);
		this.setOnMouseReleased(this::onMouseReleased);
		this.setOnMouseDragged(this::onMouseDragged);
		this.setOnScroll(this::onScroll);

		draw(context);

	}

	public void setSize(double width, double height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
		this.width = (int) width;
		this.height = (int) height;
		draw(context);
	}

	private void onScroll(ScrollEvent event) {
		scale -= event.getDeltaY() / 100;
		scale = scale < MAX_SCALE ? (scale > MIN_SCALE ? scale : MIN_SCALE) : MAX_SCALE;
		System.out.println(event.getDeltaY());
		draw(context);
	}

	private void onMousePressed(MouseEvent event) {
		System.out.println(event.getX() + " " + event.getY());

		firstMouseLocation = new Point2f((float) event.getX(), (float) event.getY());

		switch (event.getButton()) {
		case PRIMARY:
			mark(event.getX(), event.getY(), true);
			break;
		case SECONDARY:
			mark(event.getX(), event.getY(), false);
			break;
		}
		draw(context);
		System.out.println("first mouse location");
	}

	private Point2i getMouseRegionBlock(int x, int z) {
		int blockX = (int) (offset.getX() + x * scale);
		int blockZ = (int) (offset.getY() + z * scale);
		Point2i block = new Point2i(blockX, blockZ);
		return Helper.regionToBlock(Helper.blockToRegion(block));
	}

	private Tile getMouseTile(int x, int z) {
		return tiles.get(getMouseRegionBlock(x, z));
	}

	private void sortPoints(Point2i a, Point2i b) {
		Point2i aa = a.clone();
		a.setX(a.getX() < b.getX() ? a.getX() : b.getX());
		a.setY(a.getY() < b.getY() ? a.getY() : b.getY());
		b.setX(aa.getX() < b.getX() ? b.getX() : aa.getX());
		b.setY(aa.getY() < b.getY() ? b.getY() : aa.getY());
	}

	private void onMouseReleased(MouseEvent event) {
		previousMouseLocation = null;
	}

	private void onMouseDragged(MouseEvent event) {
		switch (event.getButton()) {
		case MIDDLE:
			Point2f mouseLocation = new Point2f((float) event.getX(), (float) event.getY());
			if (previousMouseLocation != null) {
				Point2f diff = mouseLocation.sub(previousMouseLocation);
				diff = diff.mul(-1);
				offset = offset.add(diff.mul(scale));
			}
			previousMouseLocation = mouseLocation;
			break;
		case PRIMARY:
			mark(event.getX(), event.getY(), true);
			break;
		case SECONDARY:
			mark(event.getX(), event.getY(), false);
			break;
		}
		draw(context);
	}

	private void mark(double mouseX, double mouseY, boolean marked) {
		Point2i regionBlock = getMouseRegionBlock((int) mouseX, (int) mouseY);
		Point2i firstRegionBlock = getMouseRegionBlock((int) firstMouseLocation.getX(), (int) firstMouseLocation.getY());
		sortPoints(firstRegionBlock, regionBlock);
		for (int x = firstRegionBlock.getX(); x <= regionBlock.getX(); x += Tile.SIZE) {
			for (int z = firstRegionBlock.getY(); z <= regionBlock.getY(); z += Tile.SIZE) {
				Tile tile = tiles.get(new Point2i(x, z));
				if (tile != null) {
					tile.mark(marked);
				}
			}
		}
	}

	/*
	*  NW  N  NE
	*    \ | /
	*  W --+-- E
	*    / | \
	*  SW  S  SE
	* */

	private void draw(GraphicsContext ctx) {
		//regionLocation is the south-west-most visible region in the window
		Point2i regionLocation = Helper.regionToBlock(Helper.blockToRegion(new Point2i((int) offset.getX(), (int) offset.getY())));

		//get all tiles that are visible inside the window
		for (int x = regionLocation.getX(); x < offset.getX() + (width * scale); x += Tile.SIZE) {
			for (int z = regionLocation.getY(); z < offset.getY() + (height * scale); z += Tile.SIZE) {
				Point2i region = new Point2i(x, z);
				if (!tiles.containsKey(region)) {
					tiles.put(region, new Tile(region));
				}
				Tile tile = tiles.get(region);

				Point2i regionOffset = region.sub((int) offset.getX(), (int) offset.getY());

				//TODO: load async

				if (!tile.isLoaded()) {
					tile.loadImage();
				}
				tile.draw(context, scale, new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale));
			}
		}
	}
}
