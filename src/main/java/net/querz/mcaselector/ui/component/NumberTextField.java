package net.querz.mcaselector.ui.component;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;

public class NumberTextField extends TextField {

	private final IntegerProperty valueProperty = new SimpleIntegerProperty();

	private EventHandler<? super ScrollEvent> onScroll;

	private final int min, max;

	public NumberTextField(int min, int max) {
		this.min = min;
		this.max = max;
		textProperty().addListener((v, o, n) -> {
			if (!n.matches("-?\\d*")) {
				setText(n.replaceAll("[^\\-\\d]", ""));
			} else if ("".equals(n)) {
				valueProperty.set(0);
			} else {
				try {
					int value = Integer.parseInt(n);
					if (value < min) {
						value = min;
					} else if (value > max) {
						value = max;
					}
					valueProperty.set(value);
					setText(value + "");
				} catch (NumberFormatException ex) {
					valueProperty.set(0);
				}
			}
		});
		setOnScroll(e -> {
			if (e.getDeltaY() > 0) {
				setText(incrementAndGet(1) + "");
			} else if (e.getDeltaY() < 0) {
				setText(incrementAndGet(-1) + "");
			}
			if (onScroll != null) {
				onScroll.handle(e);
			}
		});
		valueProperty.addListener((v, o, n) -> {
			if (n.intValue() == 0) {
				setText("");
			} else {
				setText(n + "");
			}
		});
	}

	public void setOnScrollEvent(EventHandler<? super ScrollEvent> value) {
		onScroll = value;
	}

	private int incrementAndGet(int inc) {
		int newValue = valueProperty.get() + inc;
		valueProperty.set(newValue);
		return newValue;
	}

	public IntegerProperty valueProperty() {
		return valueProperty;
	}

	public int getValue() {
		return valueProperty.get();
	}
}
