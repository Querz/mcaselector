package net.querz.mcaselector.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Translation;

public class GotoDialog extends Dialog<Point2i> {

	private Point2i value;
	private Integer x, z;
	private TextField xValue, zValue;

	public GotoDialog(Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_GOTO_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("goto-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		setResultConverter(p -> value);

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

		HBox box = new HBox();
		box.getStyleClass().add("coordinate-box");

		xValue = new TextField();
		xValue.setPromptText("X");
		xValue.textProperty().addListener((a, o, n) -> onXInput(o, n));
		zValue = new TextField();
		zValue.setPromptText("Z");
		zValue.textProperty().addListener((a, o, n) -> onZInput(o, n));

		box.getChildren().addAll(xValue, zValue);
		getDialogPane().setContent(box);

		xValue.requestFocus();
	}

	//allow empty textfield
	//allow only + / -

	private void onXInput(String o, String n) {
		try {
			x = Integer.parseInt(n);
		} catch (NumberFormatException ex) {
			x = null;
			if (!"-".equals(n) && !"+".equals(n) && !"".equals(n)) {
				xValue.setText(o);
			}
		}
		setValue();
	}

	private void onZInput(String o, String n) {
		try {
			z = Integer.parseInt(n);
		} catch (NumberFormatException ex) {
			z = null;
			if (!"-".equals(n) && !"+".equals(n) && !"".equals(n)) {
				zValue.setText(o);
			}
		}
		setValue();
	}

	private void setValue() {
		if (x == null || z == null) {
			value = null;
			getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
		} else {
			value = new Point2i(x, z);
			getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
		}
	}
}
