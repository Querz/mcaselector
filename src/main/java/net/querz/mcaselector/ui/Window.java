package net.querz.mcaselector.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.io.FileHelper;

import java.io.File;
import java.net.URL;
import java.util.*;

public class Window extends Application {

	private int width = 800, height = 600;
//	private int width = 300, height = 300;

	private Set<KeyCode> pressedKeys = new HashSet<>();

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("MCA Selector");
		primaryStage.getIcons().add(FileHelper.getIconFromResources("img/icon"));

		TileMap tileMap = new TileMap(this, width, height);

		BorderPane pane = new BorderPane();

		//menu bar
		OptionBar optionBar = new OptionBar(tileMap, primaryStage);
		pane.setTop(optionBar);

		//tilemap
		TileMapBox tileMapBox = new TileMapBox(tileMap, primaryStage);
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
//		primaryStage.show();

		// ---------------------------------------------------------------

		Map<Point2i, Set<Point2i>> marked = new HashMap<>();
		Set<Point2i> chunk = new HashSet<>();
		chunk.add(new Point2i(-17, -15));
		marked.put(new Point2i(-1, -1), chunk);

		Config.setWorldDir(new File("/Users/rb/Library/Application Support/minecraft/saves/test2/region"));
		tileMap.update();
		tileMap.setMarkedChunks(marked);
		DialogHelper.editNBT(tileMap, primaryStage);
	}

	public boolean isKeyPressed(KeyCode keyCode) {
		return pressedKeys.contains(keyCode);
	}
}
