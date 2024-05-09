package net.querz.mcaselector.ui.component;

import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import net.querz.mcaselector.point.Point2i;
import java.util.function.Consumer;

public class LocationInput extends HBox {

	private Point2i value;
	private Integer x, z;
	private final TextField xValue, zValue;

	private Consumer<Boolean> validityCheckAction;

	private final boolean emptyIsZero;

	public LocationInput(boolean emptyIsZero) {
		getStyleClass().add("location-input");

		getStylesheets().add(LocationInput.class.getClassLoader().getResource("style/component/location-input.css").toExternalForm());

		this.emptyIsZero = emptyIsZero;

		if (emptyIsZero) {
			x = z = 0;
			value = new Point2i(0, 0);
		}

		xValue = new TextField();
		xValue.setPromptText("X");
		xValue.textProperty().addListener((a, o, n) -> onXInput(o, n));
		zValue = new TextField();
		zValue.setPromptText("Z");
		zValue.textProperty().addListener((a, o, n) -> onZInput(o, n));

		getChildren().addAll(xValue, zValue);
	}

	public boolean emptyIsZero() {
		return emptyIsZero;
	}

	public void setX(Integer x) {
		this.x = x;
		if (x != null) {
			xValue.setText(x.toString());
		}
		setValue();
	}

	public void setZ(Integer z) {
		this.z = z;
		if (z != null) {
			zValue.setText(z.toString());
		}
		setValue();
	}

	public void requestFocus() {
		xValue.requestFocus();
	}

	// allow empty textfield
	// allow only + / -

	private void onXInput(String o, String n) {
		if (emptyIsZero && (n.isEmpty() || "-".equals(n) || "+".equals(n))) {
			x = 0;
			setValue();
			return;
		}
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
		if (emptyIsZero && (n.isEmpty() || "-".equals(n) || "+".equals(n))) {
			z = 0;
			setValue();
			return;
		}
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

	public Point2i getValue() {
		return value;
	}

	public void setOnValidityCheck(Consumer<Boolean> action) {
		validityCheckAction = action;
	}

	private void setValue() {
		if (x == null || z == null) {
			value = null;
			if (validityCheckAction != null) {
				validityCheckAction.accept(false);
			}
		} else {
			value = new Point2i(x, z);
			if (validityCheckAction != null) {
				validityCheckAction.accept(true);
			}
		}
	}
}
