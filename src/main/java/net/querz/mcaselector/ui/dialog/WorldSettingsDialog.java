package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.DialogHelper;
import net.querz.mcaselector.ui.UIFactory;
import java.io.File;

public class WorldSettingsDialog extends Dialog<WorldDirectories> {

	private final Label poiLabel = UIFactory.label(Translation.DIALOG_WORLD_SETTINGS_POI);
	private final Label entitiesLabel = UIFactory.label(Translation.DIALOG_WORLD_SETTINGS_ENTITIES);
	private final TextField poiDir = new TextField();
	private final TextField entitiesDir = new TextField();
	private final Image openIcon = FileHelper.getIconFromResources("img/folder");
	private final Button openPoi = new Button(null, new ImageView(openIcon));
	private final Button openEntities = new Button(null, new ImageView(openIcon));

	private final WorldDirectories worldDirectories;

	public WorldSettingsDialog(Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_WORLD_SETTINGS_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("world-settings-dialog-pane");
		getDialogPane().getScene().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		// settings dialog to set poi and entities folders manually
		// make sure that they are not equal to the region folder or to each other

		worldDirectories = Config.getWorldDirs().clone();
		setResultConverter(c -> c == ButtonType.OK ? worldDirectories : null);

		openPoi.setOnAction(e -> {
			File poi = openDirectory(poiDir);
			if (poi != null) {
				worldDirectories.setPoi(poi);
			}
		});

		openEntities.setOnAction(e -> {
			File entities = openDirectory(entitiesDir);
			if (entities != null) {
				worldDirectories.setEntities(entities);
			}
		});

		if (worldDirectories.getPoi() != null) {
			poiDir.setText(worldDirectories.getPoi() + "");
		}

		if (worldDirectories.getEntities() != null) {
			entitiesDir.setText(worldDirectories.getEntities() + "");
		}

		GridPane grid = new GridPane();
		grid.getStyleClass().add("grid-pane");
		grid.add(poiLabel, 0, 0);
		grid.add(entitiesLabel, 0, 1);
		grid.add(wrapTextFieldAndButton(poiDir, openPoi), 1, 0);
		grid.add(wrapTextFieldAndButton(entitiesDir, openEntities), 1, 1);

		getDialogPane().setContent(grid);
	}

	private HBox wrapTextFieldAndButton(TextField textField, Button button) {
		HBox box = new HBox();
		box.getStyleClass().add("h-box");
		box.getChildren().addAll(textField, button);
		return box;
	}

	private File openDirectory(TextField textField) {
		String lastOpenDirectory = FileHelper.getLastOpenedDirectory("open_world", Config.getMCSavesDir());
		File file = DialogHelper.createDirectoryChooser(lastOpenDirectory).showDialog(getDialogPane().getScene().getWindow());
		if (file != null && file.isDirectory()) {
			textField.setText(file.toString());
			return file;
		}
		return null;
	}
}
