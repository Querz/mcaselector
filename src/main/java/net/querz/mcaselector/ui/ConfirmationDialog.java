package net.querz.mcaselector.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ConfirmationDialog extends Alert {

	public ConfirmationDialog(Stage primaryStage, String title, String headerText, String cssPrefix) {
		super(
				AlertType.WARNING,
				"Are you sure?",
				ButtonType.OK,
				ButtonType.CANCEL
		);
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add(cssPrefix + "-confirmation-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		setTitle(title);
		setHeaderText(headerText);
	}
}
