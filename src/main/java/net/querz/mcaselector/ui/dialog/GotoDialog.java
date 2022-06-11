package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.component.LocationInput;

public class GotoDialog extends Dialog<Point2i> {

	public GotoDialog(Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_GOTO_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("goto-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		LocationInput locationInput = new LocationInput(false);

		setResultConverter(p -> p == ButtonType.OK ? locationInput.getValue() : null);

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.OK).setDisable(!locationInput.emptyIsZero());

		locationInput.setOnValidityCheck(valid -> getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid));

		getDialogPane().setContent(locationInput);

		locationInput.requestFocus();
	}
}
