package net.querz.mcaselector.tiles;

import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import net.querz.mcaselector.Point;


public class TileMap extends Group {
	private float scale = 2;

	public static final float MAX_SCALE = 5;
	public static final float MIN_SCALE = 0.2f;

	private Canvas canvas;
	private GraphicsContext context;

	private Point offset = new Point();

	private Point previousMouseLocation = null;

	public TileMap() {
		canvas = new Canvas(300, 300);

		context = canvas.getGraphicsContext2D();

		this.getChildren().add(canvas);

		this.setOnMouseReleased(this::onMouseReleased);
		this.setOnMouseDragged(this::onMouseDragged);
		this.setOnScroll(this::onScroll);

		drawMap(context);

	}

	private void onScroll(ScrollEvent event) {
		scale += event.getDeltaY() / 50;
		scale = scale < MAX_SCALE ? (scale > MIN_SCALE ? scale : MIN_SCALE) : MAX_SCALE;
		System.out.println(event.getDeltaY());
		drawMap(context);
	}

	private void onMouseReleased(MouseEvent event) {
		previousMouseLocation = null;
	}

	private void onMouseDragged(MouseEvent event) {
		Point mouseLocation = new Point((int) event.getX(), (int) event.getY());
		if (previousMouseLocation != null) {
			Point diff = mouseLocation.sub(previousMouseLocation);
			offset = offset.add(diff.div(scale));

			System.out.println(offset);

			drawMap(context);

		}
		previousMouseLocation = mouseLocation;
	}

	private void drawMap(GraphicsContext gc) {
		Point off = new Point((int) ((offset.getX() % 32) * scale), (int) ((offset.getY() % 32) * scale));
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
