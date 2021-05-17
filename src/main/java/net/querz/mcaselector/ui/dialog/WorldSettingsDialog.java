package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.FileTextField;
import net.querz.mcaselector.ui.UIFactory;

public class WorldSettingsDialog extends Dialog<WorldSettingsDialog.Result> {

	private final Label poiLabel = UIFactory.label(Translation.DIALOG_WORLD_SETTINGS_POI);
	private final Label entitiesLabel = UIFactory.label(Translation.DIALOG_WORLD_SETTINGS_ENTITIES);
	private final Label heightLabel = UIFactory.label(Translation.DIALOG_WORLD_SETTINGS_RENDER_HEIGHT);
	private final Label layerOnlyLabel = UIFactory.label(Translation.DIALOG_WORLD_SETTINGS_RENDER_LAYER_ONLY);
	private final FileTextField poiField = new FileTextField();
	private final FileTextField entitiesField = new FileTextField();
	private final Slider heightSlider = new Slider(-64, 319, 319);
	private final CheckBox layerOnlyCheckBox = new CheckBox();

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
		setResultConverter(c -> c == ButtonType.OK ? new Result(worldDirectories, (int) heightSlider.getValue(), layerOnlyCheckBox.isSelected()) : null);

		poiField.setFile(worldDirectories.getPoi());
		entitiesField.setFile(worldDirectories.getEntities());

		HBox heightBox = new HBox();
		heightBox.getStyleClass().add("height-box");
		heightBox.getChildren().addAll(heightSlider, UIFactory.attachTextFieldToSlider(heightSlider));
		heightSlider.setValue(Config.getRenderHeight());
		heightSlider.setSnapToTicks(true);
		heightSlider.setMajorTickUnit(32);
		heightSlider.setMinorTickCount(384);

		layerOnlyCheckBox.setSelected(Config.renderLayerOnly());

		GridPane grid = new GridPane();
		grid.getStyleClass().add("grid-pane");
		grid.add(poiLabel, 0, 0);
		grid.add(entitiesLabel, 0, 1);
		grid.add(heightLabel, 0, 2);
		grid.add(layerOnlyLabel, 0, 3);
		grid.add(poiField, 1, 0);
		grid.add(entitiesField, 1, 1);
		grid.add(heightBox, 1, 2);
		grid.add(layerOnlyCheckBox, 1, 3);

		getDialogPane().setContent(grid);
	}

	public static class Result {

		private final WorldDirectories worldDirectories;
		private final int height;
		private final boolean layerOnly;

		private Result(WorldDirectories worldDirectories, int height, boolean layerOnly) {
			this.worldDirectories = worldDirectories;
			this.height = height;
			this.layerOnly = layerOnly;
		}

		public WorldDirectories getWorldDirectories() {
			return worldDirectories;
		}

		public int getHeight() {
			return height;
		}

		public boolean layerOnly() {
			return layerOnly;
		}
	}
}
