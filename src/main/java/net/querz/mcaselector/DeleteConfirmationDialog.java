package net.querz.mcaselector;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import net.querz.mcaselector.tiles.TileMap;

public class DeleteConfirmationDialog extends Alert {

	public DeleteConfirmationDialog(TileMap tileMap) {
		super(
			AlertType.WARNING,
			"Are you sure?",
			ButtonType.OK,
			ButtonType.CANCEL
		);
		setTitle("Delete Selection");
		setHeaderText("You are about to delete " + tileMap.getSelectedChunks() + " chunks from this world.");
	}
}
