package net.querz.mcaselector.ui.dialog;

import javafx.stage.Stage;
import net.querz.mcaselector.selection.SelectionData;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tile.TileMap;

public class ImageExportConfirmationDialog extends ConfirmationDialog {

	public ImageExportConfirmationDialog(TileMap tileMap, SelectionData data, Stage primaryStage) {
		super(
				primaryStage,
				Translation.DIALOG_IMAGE_EXPORT_CONFIRMATION_TITLE,
				Translation.DIALOG_IMAGE_EXPORT_CONFIRMATION_HEADER_SHORT,
				"export"
		);

		headerTextProperty().unbind();
		setHeaderText(String.format(Translation.DIALOG_IMAGE_EXPORT_CONFIRMATION_HEADER_VERBOSE.toString(), data.getWidth() * 16, data.getHeight() * 16));
		tileMap.releaseAllKeys();
	}
}
