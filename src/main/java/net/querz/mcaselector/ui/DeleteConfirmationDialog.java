package net.querz.mcaselector.ui;

import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Translation;

public class DeleteConfirmationDialog extends ConfirmationDialog {

	public DeleteConfirmationDialog(TileMap tileMap, Stage primaryStage) {
		super(
				primaryStage,
				Translation.DIALOG_DELETE_CHUNKS_CONFIRMATION_TITLE,
				Translation.DIALOG_DELETE_CHUNKS_CONFIRMATION_HEADER_SHORT,
				"delete"
		);

		if (tileMap != null) {
			headerTextProperty().unbind();
			setHeaderText(String.format(Translation.DIALOG_DELETE_CHUNKS_CONFIRMATION_HEADER_VERBOSE.toString(), tileMap.getSelectedChunks()));
		}
	}
}
