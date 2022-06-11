package net.querz.mcaselector.ui.dialog;

import javafx.stage.Stage;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.text.Translation;

public class DeleteConfirmationDialog extends ConfirmationDialog {

	public DeleteConfirmationDialog(TileMap tileMap, Stage primaryStage) {
		super(
				primaryStage,
				Translation.DIALOG_DELETE_CHUNKS_CONFIRMATION_TITLE,
				Translation.DIALOG_DELETE_CHUNKS_CONFIRMATION_HEADER_SHORT,
				"delete"
		);

		if (tileMap != null) {
			if (!tileMap.getSelection().isInverted()) {
				headerTextProperty().unbind();
				setHeaderText(String.format(Translation.DIALOG_DELETE_CHUNKS_CONFIRMATION_HEADER_VERBOSE.toString(), tileMap.getSelectedChunks()));
			}
			tileMap.releaseAllKeys();
		}
	}
}
