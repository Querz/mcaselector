package net.querz.mcaselector.ui.component;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;

public class NumberTextField extends TextField {

	private final LongProperty valueProperty = new SimpleLongProperty();

	private EventHandler<? super ScrollEvent> onScroll;

	public NumberTextField(long min, long max) {
		textProperty().addListener((v, o, n) -> {
			if (!n.matches("-?\\d*")) {
				setText(n.replaceAll("[^\\-\\d]", ""));
			} else if (n.isEmpty()) {
				valueProperty.set(0);
			} else {
				try {
					long value = Integer.parseInt(n);
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

	private long incrementAndGet(long inc) {
		long newValue = valueProperty.get() + inc;
		valueProperty.set(newValue);
		return newValue;
	}

	public LongProperty valueProperty() {
		return valueProperty;
	}

	public long getValue() {
		return valueProperty.get();
	}
}
