package net.querz.mcaselector.ui.component;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

public class HeightSlider extends HBox {

	private final IntegerProperty valueProperty = new SimpleIntegerProperty();

	private final Slider slider = new Slider(-64, 319, 319);
	private final Label heightMinLabel = new Label("-64");
	private final Label heightMaxLabel = new Label("319");
	private final NumberTextField heightField = new NumberTextField(-64, 2047);

	/*
	* when scrolling on slider: always set value in text field if slider value changed
	* when scrolling on text field: set slider value if text field value is in range of slider
	* when typing on text field: set slider value if text field value is in range of slider.
	*   if text field value is larger than slider max, set slider to max
	*   if text field value is smaller than slider min, set slider to min
	* */

	public HeightSlider(int init, boolean showCustomlabels) {
		valueProperty.set(init);
		heightField.getStyleClass().add("slider-value-field");
		slider.setSnapToTicks(true);
		slider.setShowTickLabels(!showCustomlabels);
		slider.setShowTickMarks(!showCustomlabels);
		slider.setMajorTickUnit(32);
		slider.setMinorTickCount(384);
		slider.setPrefWidth(300);
		slider.setBlockIncrement(1);
		slider.setValue(init);

		heightField.valueProperty().set(init);

		if (showCustomlabels) {
			slider.setLabelFormatter(new StringConverter<>() {
				@Override
				public String toString(Double object) {
					return null;
				}

				@Override
				public Double fromString(String string) {
					return null;
				}
			});
		}

		slider.setOnScroll(e -> {
			int delta = e.getDeltaY() > 0 ? 1 : -1;
			slider.setValue(slider.getValue() + delta);
		});

		slider.valueProperty().addListener((v, o, n) -> {
			if (o.intValue() != n.intValue()) {
				heightField.setText(n.intValue() + "");
			}
		});

		heightField.valueProperty().addListener((v, o, n) -> {
			if (o.intValue() != n.intValue()) {
				valueProperty.set(n.intValue());
				if (n.intValue() > slider.getMax()) {
					slider.setValue(319);
				} else if (n.intValue() < slider.getMin()) {
					slider.setValue(-64);
				} else {
					slider.setValue(n.intValue());
				}
			}
		});

		// value property sets text field value
		valueProperty.addListener((v, o, n) -> heightField.setText(n.intValue() + ""));

		if (showCustomlabels) {
			getChildren().addAll(heightMinLabel, slider, heightMaxLabel, heightField);
		} else {
			getChildren().addAll(slider, heightField);
		}
	}

	public IntegerProperty valueProperty() {
		return valueProperty;
	}

	public int getValue() {
		return valueProperty.get();
	}

	public void disable(boolean disable) {
		slider.setDisable(disable);
		heightMinLabel.setDisable(disable);
		heightMaxLabel.setDisable(disable);
		heightField.setDisable(disable);
	}

	public void setMajorTickUnit(int unit) {
		slider.setMajorTickUnit(unit);
	}
}
