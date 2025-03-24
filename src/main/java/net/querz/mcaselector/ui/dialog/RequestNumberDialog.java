package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.component.NumberTextField;

public class RequestNumberDialog extends Dialog<Long> {

	public RequestNumberDialog(Stage primaryStage, Translation title, long min, long max) {
		titleProperty().bind(title.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("request-number-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		NumberTextField numberTextField = new NumberTextField(min, max);
		setResultConverter(n -> n == ButtonType.OK ? numberTextField.getValue() : null);

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		getDialogPane().setPadding(new Insets(20));

		getDialogPane().setContent(numberTextField);

		Platform.runLater(numberTextField::requestFocus);
	}
}
