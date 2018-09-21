package net.querz.mcaselector.ui;

import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;

public class ExportConfirmationDialog extends ConfirmationDialog {

	public ExportConfirmationDialog(TileMap tileMap, Stage primaryStage) {
		super(
			primaryStage,
			"Export chunks",
			tileMap == null ? "You are about to export an unknown number of chunks from this world." :
					"You are about to export " + tileMap.getSelectedChunks() + " chunks from this world.",
			"export"
		);
	}
}
