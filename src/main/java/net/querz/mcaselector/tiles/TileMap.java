package net.querz.mcaselector.tiles;

import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import net.querz.mcaselector.Point;


public class TileMap extends Group {
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

	}

	private void onMouseReleased(MouseEvent event) {
		previousMouseLocation = null;
	}

	private void onMouseDragged(MouseEvent event) {
		Point mouseLocation = new Point((int) event.getX(), (int) event.getY());
		if (previousMouseLocation != null) {
			Point diff = mouseLocation.sub(previousMouseLocation);
			offset = offset.add(diff);

			System.out.println(offset);

			drawMap(context);

		}
		previousMouseLocation = mouseLocation;
	}

	private void drawMap(GraphicsContext gc) {
		Point off = new Point(offset.getX() % 32, offset.getY() % 32);
		for (int x = 0; x < 23; x++) {
			for (int z = 0; z < 23; z++) {

				// draw a chessboard for testing
				if (x % 2 == 0 && z % 2 == 0 || x % 2 == 1 && z % 2 == 1) {
					gc.setFill(Color.RED);
					gc.fillRect(off.getX() + (x - 2) * 16, off.getY() + (z - 2) * 16, 16, 16);
				} else {
					gc.setFill(Color.BLACK);
					gc.fillRect(off.getX() + (x - 2) * 16, off.getY() + (z - 2) * 16, 16, 16);
				}
			}
		}
	}
}
