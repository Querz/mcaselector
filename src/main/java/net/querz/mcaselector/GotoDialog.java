package net.querz.mcaselector;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import net.querz.mcaselector.util.Point2i;

public class GotoDialog extends Dialog<Point2i> {

	private Point2i value;
	private TextField xValue, zValue;

	public GotoDialog() {
		setTitle("Goto location");
		setResultConverter(p -> value);

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

		HBox box = new HBox();

		xValue = new TextField();
		xValue.setPromptText("X");
		xValue.textProperty().addListener((a, b, c) -> onInput(c, zValue.getText()));
		zValue = new TextField();
		zValue.setPromptText("Z");
		zValue.textProperty().addListener((a, b, c) -> onInput(xValue.getText(), c));

		box.getChildren().addAll(xValue, zValue);
		getDialogPane().setContent(box);

		xValue.requestFocus();
	}

	private void onInput(String xValue, String zValue) {
		try {
			value = new Point2i(Integer.parseInt(xValue), Integer.parseInt(zValue));
			getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
		} catch (NumberFormatException ex) {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
			value = null;
		}
	}
}
