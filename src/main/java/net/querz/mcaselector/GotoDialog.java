package net.querz.mcaselector;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import net.querz.mcaselector.util.Point2i;

public class GotoDialog extends Dialog<Point2i> {

	private Point2i value;

	public GotoDialog() {
		setTitle("Goto location");
		setResultConverter(p -> value);

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

		HBox box = new HBox();

		TextField xValue = new TextField();
		xValue.setPromptText("X");
		xValue.textProperty().addListener((a, b, c) -> onXInput(c));
		TextField zValue = new TextField();
		zValue.setPromptText("Z");
		zValue.textProperty().addListener((a, b, c) -> onZInput(c));

		box.getChildren().addAll(xValue, zValue);

		getDialogPane().setContent(box);

		xValue.requestFocus();
	}

	//TODO: allow negative numbers
	private void onXInput(String newValue) {
		try {
			value.setX(Integer.parseInt(newValue));
			getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
		} catch (NumberFormatException ex) {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
		}
	}

	private void onZInput(String newValue) {
		try {
			value.setY(Integer.parseInt(newValue));
			getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
		} catch (NumberFormatException ex) {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
		}
	}
}
