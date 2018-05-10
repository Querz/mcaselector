package net.querz.mcaselector;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;

public class Window extends Application {

	private int width = 800, height = 600;

	@Override
	public void start(Stage primaryStage) {
		TileMap tileMap = new TileMap(width, height);

		Scene scene = new Scene(tileMap, width, height);

		ChangeListener<Number> sizeListener = (o, r, n) -> tileMap.setSize(primaryStage.getWidth(), primaryStage.getHeight());

		primaryStage.widthProperty().addListener(sizeListener);
		primaryStage.heightProperty().addListener(sizeListener);

		primaryStage.setScene(scene);
		primaryStage.show();
	}
}
