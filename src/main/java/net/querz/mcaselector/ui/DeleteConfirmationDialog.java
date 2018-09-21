package net.querz.mcaselector.ui;

import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;

public class DeleteConfirmationDialog extends ConfirmationDialog {

	public DeleteConfirmationDialog(TileMap tileMap, Stage primaryStage) {
		super(
			primaryStage,
			"Delete chunks",
			tileMap == null ? "You are about to delete an unknown number of chunks from this world." :
					"You are about to delete " + tileMap.getSelectedChunks() + " chunks from this world.",
			"delete"
		);
	}
}
