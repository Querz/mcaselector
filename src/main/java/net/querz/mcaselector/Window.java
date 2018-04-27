package net.querz.mcaselector;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;

public class Window extends Application {

	private GridPane gridPane;
	private int viewportX, viewwportZ;
	private int gridWidth = 128, gridHeight = 128; // dimensions in chunks (4x4 region files)

	@Override
	public void start(Stage primaryStage) {
		gridPane = new GridPane();

//		for (int x = 0; x < gridWidth; x++) {
//			for (int y = 0; y < gridHeight; y++) {
//
//			}
//		}

		Scene scene = new Scene(new TileMap(), 300, 300);

		primaryStage.setScene(scene);
		primaryStage.show();
	}
}
