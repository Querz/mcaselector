package net.querz.mcaselector;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.tiles.TileMap;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class Window extends Application {

//	private int width = 800, height = 600;
	private int width = 300, height = 300;

	private Set<KeyCode> pressedKeys = new HashSet<>();

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("MCA Selector");

		TileMap tileMap = new TileMap(this, width, height);

		BorderPane pane = new BorderPane();

		//menu bar
		pane.setTop(new OptionBar(tileMap, primaryStage));

		//tilemap
		HBox tileMapBox = new HBox();
		ChangeListener<Number> sizeListener = (o, r, n) -> {
			tileMap.resize(primaryStage.getWidth(), primaryStage.getHeight());
			System.out.println("resize to " + primaryStage.getWidth() + " " + primaryStage.getHeight());
		};
		primaryStage.widthProperty().addListener(sizeListener);
		primaryStage.heightProperty().addListener(sizeListener);

		tileMapBox.setAlignment(Pos.TOP_LEFT);
		tileMapBox.getChildren().add(tileMap);

		pane.setCenter(tileMapBox);

		//status bar
		pane.setBottom(new StatusBar(tileMap));

		Scene scene = new Scene(pane, width, height);

		URL cssRes = Window.class.getClassLoader().getResource("style.css");
		if (cssRes != null) {
			String styleSheet = cssRes.toExternalForm();
			scene.getStylesheets().add(styleSheet);
		}

		scene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
		scene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));

		primaryStage.setOnCloseRequest(e -> System.exit(0));
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public boolean isKeyPressed(KeyCode keyCode) {
		return pressedKeys.contains(keyCode);
	}
}
