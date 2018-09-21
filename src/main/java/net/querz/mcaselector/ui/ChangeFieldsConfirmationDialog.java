package net.querz.mcaselector.ui;

import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;

public class ChangeFieldsConfirmationDialog extends ConfirmationDialog {

	public ChangeFieldsConfirmationDialog(TileMap tileMap, Stage primaryStage) {
		super(
				primaryStage,
				"Change chunks",
				tileMap == null ? "You are about to change data in an unknown number of chunks in this world." :
						"You are about to change data in " + tileMap.getSelectedChunks() + " chunks in this world.",
				"change"
		);
	}
}
