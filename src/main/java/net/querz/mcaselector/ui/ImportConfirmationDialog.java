package net.querz.mcaselector.ui;

import javafx.stage.Stage;

public class ImportConfirmationDialog extends ConfirmationDialog {

	public ImportConfirmationDialog(Stage primaryStage) {
		super(
				primaryStage,
				"Import chunks",
				"You are about to import an unknown number of chunks to this world. This will overwrite any duplicate chunks existing in the current world.",
				"import"
		);
	}
}
