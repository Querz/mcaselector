package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.TileMap;
import java.io.File;
import java.util.List;

public class SelectWorldDialog extends Dialog<File> {

	private final ListView<File> worlds = new ListView<>();

	public SelectWorldDialog(List<File> worldDirectories, TileMap tileMap, Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_SELECT_WORLD_TITLE.getProperty());

		initStyle(StageStyle.UTILITY);

		getDialogPane().getStyleClass().add("select-world-dialog-pane");

		worlds.setEditable(false);
		worlds.setCellFactory(view -> new FileNameListCell());
		worlds.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		worlds.getItems().addAll(worldDirectories);
		worlds.getSelectionModel().selectFirst();

		setResultConverter(p -> worlds.getSelectionModel().getSelectedItem());

		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		getDialogPane().setContent(worlds);
	}

	private static class FileNameListCell extends ListCell<File> {
		@Override
		protected void updateItem(File file, boolean empty) {
			super.updateItem(file, empty);
			if (empty || file == null) {
				setText(null);
			} else {
				setText(file.getName());
			}
			setGraphic(null);
		}
	}
}
