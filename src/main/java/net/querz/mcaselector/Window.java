package net.querz.mcaselector;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;

public class Window extends Application {

	private int width = 800, height = 600;

	@Override
	public void start(Stage primaryStage) {

		TileMap tileMap = new TileMap(width, height);

		BorderPane pane = new BorderPane();

		//menu bar
		pane.setTop(new OptionBar(tileMap));

		//tilemap
		HBox tileMapBox = new HBox();
		ChangeListener<Number> sizeListener = (o, r, n) -> tileMap.resize(primaryStage.getScene().getWidth(), primaryStage.getScene().getHeight());
		primaryStage.widthProperty().addListener(sizeListener);
		primaryStage.heightProperty().addListener(sizeListener);
		tileMapBox.setAlignment(Pos.TOP_LEFT);
		tileMapBox.getChildren().add(tileMap);

		pane.setCenter(tileMapBox);

		//status bar
		pane.setBottom(new StatusBar(tileMap));

		Scene scene = new Scene(pane, width, height);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
}
