package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import javafx.stage.StageStyle;
import net.querz.mcaselector.text.Translation;

public class ConfirmationDialog extends Alert {

	public ConfirmationDialog(Window owner, Translation title, Translation headerText, String cssPrefix) {
		super(
				AlertType.WARNING,
				"",
				ButtonType.OK,
				ButtonType.CANCEL
		);
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add(cssPrefix + "-confirmation-dialog-pane");
		getDialogPane().getStylesheets().addAll(owner.getScene().getStylesheets());
		titleProperty().bind(title.getProperty());
		headerTextProperty().bind(headerText.getProperty());
		contentTextProperty().bind(Translation.DIALOG_CONFIRMATION_QUESTION.getProperty());
	}
}
