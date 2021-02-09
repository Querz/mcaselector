package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class PreviewDisclaimerDialog extends Alert {

	public PreviewDisclaimerDialog(Stage primaryStage) {
		super(AlertType.WARNING, "This is a preview version.\nUse at your own risk.\nMake backups.", ButtonType.OK);
		initStyle(StageStyle.UTILITY);
		setTitle("Preview");
		setHeaderText("WARNING");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
	}
}
