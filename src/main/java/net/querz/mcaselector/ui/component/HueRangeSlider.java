package net.querz.mcaselector.ui.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import net.querz.mcaselector.io.ImageHelper;

public class HueRangeSlider extends RangeSlider {

	private final BooleanProperty invertedProperty = new SimpleBooleanProperty(false);
	private final int width;

	public HueRangeSlider(float minHue, float maxHue, float low, float high, int width) {
		super(minHue, maxHue, minHue, maxHue);
		getStylesheets().add(HueRangeSlider.class.getClassLoader().getResource("style/component/hue-range-slider.css").toExternalForm());
		this.width = width;

		if (low > high) {
			invertedProperty.set(true);
			low = maxHue - low;
			high = maxHue - high;
		}

		setLow(low);
		setHigh(high);

		getStyleClass().add("hue-range-slider");

		// create rainbow
		renderBackground();

		highProperty().addListener((v, o, n) -> renderBackground());
		lowProperty().addListener((v, o, n) -> renderBackground());

		invertedProperty.addListener((v, o, n) -> {
			double l = getLow();
			double h = getHigh();
			if (n) {
				setHigh(getMax() - l);
				setLow(getMax() - h);
			} else {
				setLow(getMax() - h);
				setHigh(getMax() - l);
			}
			renderBackground();
		});
	}

	public void setInverted(boolean inverted) {
		invertedProperty.set(inverted);
	}

	public boolean isInverted() {
		return invertedProperty.get();
	}

	public float getMinHue() {
		if (invertedProperty.get()) {
			return (float) getMax() - (float) getLow();
		}
		return (float) getLow();
	}

	public float getMaxHue() {
		if (invertedProperty.get()) {
			return (float) getMax() - (float) getHigh();
		}
		return (float) getHigh();
	}

	private void renderBackground() {
		Image bg = ImageHelper.renderGradient(width, 0, (float) getMax(), (float) getLow(), (float) getHigh(), invertedProperty.get());
		BackgroundImage bgi = new BackgroundImage(bg, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
		setBackground(new Background(bgi));
	}
}
