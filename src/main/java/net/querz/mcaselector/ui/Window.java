package net.querz.mcaselector.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.ui.dialog.EditArrayDialog;
import net.querz.mcaselector.ui.dialog.PreviewDisclaimerDialog;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Window extends Application {

	private final int width = 800, height = 600;

	private final Set<KeyCode> pressedKeys = new HashSet<>();

	private Stage primaryStage;
	private String title = "";
	private OptionBar optionBar;

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
		optionBar = new OptionBar(tileMap, primaryStage);
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
		long[] arr = new long[]{
				0b1111_1110_1101_1100_1011_1010_1001_1000_0111_0110_0101_0100_0011_0010_0001_0000L,
				0b0000_01011_01010_01001_01000_00111_00110_00101_00100_00011_00010_00001_00000L,
				0b0000_001000_000111_000110_000110_000101_000100_000011_000010_000001_000000L,
				0b0_0001000_0000111_0000110_0000101_0000100_0000011_0000010_0000001_0000000L,
				0b00000111_00000110_00000101_00000100_00000011_00000010_00000001_00000000L,
				0b0_000000110_000000101_000000100_000000011_000000010_000000001_000000000L,
				0b0000_0000000101_0000000100_0000000011_0000000010_0000000001_0000000000L,
				0b000000000_00000000100_00000000011_00000000010_00000000001_00000000000L,
				0b0000_000000000100_000000000011_000000000010_000000000001_000000000000L,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
		};

		EditArrayDialog<long[]> diag = new EditArrayDialog<>(arr, primaryStage);
		diag.setResizable(true);
		diag.showAndWait();

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
}
