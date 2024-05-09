package net.querz.mcaselector.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.ui.component.OptionBar;
import net.querz.mcaselector.ui.component.StatusBar;
import net.querz.mcaselector.ui.component.TileMapBox;
import net.querz.mcaselector.ui.dialog.PreviewDisclaimerDialog;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Window extends Application {

	private final int width = 800, height = 600;

	private final Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);

	private Stage primaryStage;
	private String title = "";
	private OptionBar optionBar;
	private TileMapBox tileMapBox;

	private final List<Dialog<?>> trackedDialogs = new ArrayList<>();

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

		// menu bar
		optionBar = new OptionBar(tileMap, primaryStage);
		pane.setTop(optionBar);

		// tilemap
		tileMapBox = new TileMapBox(tileMap, primaryStage);
		pane.setCenter(tileMapBox);

		// status bar
		pane.setBottom(new StatusBar(tileMap));

		Scene scene = new Scene(pane, width, height);

		Font.loadFont(Window.class.getClassLoader().getResource("font/NotoSans-Regular.ttf").toExternalForm(), 10);
		Font.loadFont(Window.class.getClassLoader().getResource("font/NotoSansMono-Regular.ttf").toExternalForm(), 10);
		Font.loadFont(Window.class.getClassLoader().getResource("font/NotoSansMono-Bold.ttf").toExternalForm(), 10);


		URL cssRes = Window.class.getClassLoader().getResource("style/base.css");
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

		primaryStage.setOnCloseRequest(e -> {
			DialogHelper.quit(tileMap, primaryStage);
			e.consume();
		});
		primaryStage.setScene(scene);

		if (version.contains("pre")) {
			new PreviewDisclaimerDialog(primaryStage).showAndWait();
		}

		tileMap.requestFocus();

		primaryStage.focusedProperty().addListener((v, o, n) -> {
			if (n) {
				trackedDialogs.forEach(d -> {
					((Stage) d.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true);
					((Stage) d.getDialogPane().getScene().getWindow()).setAlwaysOnTop(false);
				});
			}
		});

		Logging.updateThreadContext();
		primaryStage.show();
	}

	public void trackDialog(Dialog<?> dialog) {
		trackedDialogs.add(dialog);
	}

	public void untrackDialog(Dialog<?> dialog) {
		trackedDialogs.remove(dialog);
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

	public OptionBar getOptionBar() {
		return optionBar;
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public TileMapBox getTileMapBox() {
		return tileMapBox;
	}
}
