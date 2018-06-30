package net.querz.mcaselector;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.tiles.TileMap;

public class DeleteConfirmationDialog extends Alert {

	public DeleteConfirmationDialog(TileMap tileMap, Stage primaryStage) {
		super(
			AlertType.WARNING,
			"Are you sure?",
			ButtonType.OK,
			ButtonType.CANCEL
		);
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("delete-confirmation-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		setTitle("Delete Selection");
		setHeaderText("You are about to delete " + tileMap.getSelectedChunks() + " chunks from this world.");
	}
}
