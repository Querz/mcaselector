package net.querz.mcaselector.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.ui.dialog.PreviewDisclaimerDialog;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Window extends Application {

	private final int width = 800, height = 600;

	private final Set<KeyCode> pressedKeys = new HashSet<>();

	private Stage primaryStage;
	private String title = "";

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		String version;
		try {
			version = FileHelper.getManifestAttributes().getValue("Application-Version");
		} catch (IOException ex) {
			version = "dev";
		}

		title = "MCA Selector " + version;
		primaryStage.setTitle(title);
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
		primaryStage.focusedProperty().addListener((obs, o, n) -> {
			if (!n) {
				pressedKeys.clear();
			}
		});

		primaryStage.setOnCloseRequest(e -> System.exit(0));
		primaryStage.setScene(scene);

		if (version.contains("pre")) {
			new PreviewDisclaimerDialog(primaryStage).showAndWait();
		}

		primaryStage.show();
	}

	public void setTitleSuffix(String suffix) {
		if (suffix == null || suffix.isEmpty()) {
			primaryStage.setTitle(title);
		} else {
			primaryStage.setTitle(title + "    " + suffix);
		}
	}

	public boolean isKeyPressed(KeyCode keyCode) {
		return pressedKeys.contains(keyCode);
	}
}
