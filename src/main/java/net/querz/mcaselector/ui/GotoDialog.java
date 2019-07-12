package net.querz.mcaselector.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Translation;

public class GotoDialog extends Dialog<Point2i> {

	public GotoDialog(Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_GOTO_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("goto-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		LocationInput locationInput = new LocationInput(true);

		setResultConverter(p -> locationInput.getValue());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.OK).setDisable(!locationInput.emptyIsZero());

		locationInput.setOnValidityCheck(valid -> getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid));

		getDialogPane().setContent(locationInput);

		locationInput.requestFocus();
	}
}
