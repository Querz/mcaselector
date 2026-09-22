package net.querz.mcaselector.ui.dialog;

import javafx.stage.Stage;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tile.TileMap;

public class DefragmentConfirmationDialog extends ConfirmationDialog {

	public DefragmentConfirmationDialog(TileMap tileMap, Stage primaryStage) {
		super(
				primaryStage,
				Translation.DIALOG_DEFRAGMENT_REGIONS_CONFIRMATION_TITLE,
				Translation.DIALOG_DEFRAGMENT_REGIONS_CONFIRMATION_HEADER_SHORT,
				"defragment"
		);

		if (tileMap != null) {
			if (!tileMap.getSelection().isInverted()) {
				headerTextProperty().unbind();
				setHeaderText(String.format(Translation.DIALOG_DEFRAGMENT_REGIONS_CONFIRMATION_HEADER_VERBOSE.toString(), tileMap.getSelection().size()));
			}
			tileMap.releaseAllKeys();
		}
	}
}
