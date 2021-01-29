package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.FileTextField;
import net.querz.mcaselector.ui.UIFactory;

public class WorldSettingsDialog extends Dialog<WorldDirectories> {

	private final Label poiLabel = UIFactory.label(Translation.DIALOG_WORLD_SETTINGS_POI);
	private final Label entitiesLabel = UIFactory.label(Translation.DIALOG_WORLD_SETTINGS_ENTITIES);
	private final FileTextField poiField = new FileTextField();
	private final FileTextField entitiesField = new FileTextField();

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

		poiField.setFile(worldDirectories.getPoi());
		entitiesField.setFile(worldDirectories.getEntities());

		GridPane grid = new GridPane();
		grid.getStyleClass().add("grid-pane");
		grid.add(poiLabel, 0, 0);
		grid.add(entitiesLabel, 0, 1);
		grid.add(poiField, 1, 0);
		grid.add(entitiesField, 1, 1);

		getDialogPane().setContent(grid);
	}
}
