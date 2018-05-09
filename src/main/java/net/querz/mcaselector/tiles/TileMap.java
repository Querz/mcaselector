package net.querz.mcaselector.tiles;

import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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

	private Map<Point2i, Tile> tiles = new HashMap<>();

	public TileMap() {
		height = width = 300;
		canvas = new Canvas(width, height);

		context = canvas.getGraphicsContext2D();

		this.getChildren().add(canvas);

		this.setOnMousePressed(this::onMousePressed);
		this.setOnMouseReleased(this::onMouseReleased);
		this.setOnMouseDragged(this::onMouseDragged);
		this.setOnScroll(this::onScroll);

		tiles.put(new Point2i(0, 0), new RegionTile(new Point2i(0, 0)));

		draw(context);

	}

	private void onScroll(ScrollEvent event) {
		scale -= event.getDeltaY() / 50;
		scale = scale < MAX_SCALE ? (scale > MIN_SCALE ? scale : MIN_SCALE) : MAX_SCALE;
		System.out.println(event.getDeltaY());
		draw(context);
	}

	private void onMousePressed(MouseEvent event) {
		System.out.println(event.getX());
	}

	private void onMouseReleased(MouseEvent event) {
		previousMouseLocation = null;
	}

	private void onMouseDragged(MouseEvent event) {
		Point2f mouseLocation = new Point2f((float) event.getX(), (float) event.getY());
		if (previousMouseLocation != null) {
			Point2f diff = mouseLocation.sub(previousMouseLocation);
			diff = diff.mul(-1);
			offset = offset.add(diff.mul(scale));

//			System.out.println("offset" + offset);

			draw(context);

		}
		previousMouseLocation = mouseLocation;
	}

	/*
	*  NW  N  NE
	*    \ | /
	*  W --+-- E
	*    / | \
	*  SW  S  SE
	* */


	//TODO: invert canvas y axis
	private void draw(GraphicsContext ctx) {
		//regionLocation is the south-west-most visible region in the window
		Point2i regionLocation = Helper.regionToBlock(Helper.blockToRegion(new Point2i((int) offset.getX(), (int) offset.getY())));
		//get all tiles that are visible inside the window
		List<Point2i> visibleRegions = new ArrayList<>();

		System.out.println("scale " + scale);

		for (int x = regionLocation.getX(); x < offset.getX() + (width * scale); x += 512) {
			for (int z = regionLocation.getY(); z < offset.getY() + (height * scale); z += 512) {
				System.out.print("adding " + x + " " + z + " ");
				visibleRegions.add(new Point2i(x, z));
			}
		}

		for (Point2i region : visibleRegions) {
			if (!tiles.containsKey(region)) {
				tiles.put(region, new RegionTile(region));
			}
			Tile tile = tiles.get(region);

			Point2i regionOffset = region.sub((int) offset.getX(), (int) offset.getY());

			//TODO: load async

			if (!tile.isLoaded()) {
//				Thread loader = new Thread(() -> {
					tile.loadImage();
//					System.out.println("loaded");
//
//					tile.draw(context, scale, new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale));
//				});
//				loader.start();
			}
			tile.draw(context, scale, new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale));
		}
		System.out.println();
	}

	private void drawMap(GraphicsContext gc) {
		Point2f off = new Point2f((int) ((offset.getX() % 32) * scale), (int) ((offset.getY() % 32) * scale));
		for (int x = 0; x < Math.ceil(24.0 / scale); x++) {
			for (int z = 0; z < Math.ceil(24.0 / scale); z++) {

				// draw a chessboard for testing
				if (x % 2 == 0 && z % 2 == 0 || x % 2 == 1 && z % 2 == 1) {
					gc.setFill(Color.RED);
					gc.fillRect(off.getX() + (x * scale - 2) * 16, off.getY() + (z * scale - 2) * 16, 16 * scale, 16 * scale);
				} else {
					gc.setFill(Color.BLACK);
					gc.fillRect(off.getX() + (x * scale - 2) * 16, off.getY() + (z * scale - 2) * 16, 16 * scale, 16 * scale);
				}
			}
		}
	}
}
