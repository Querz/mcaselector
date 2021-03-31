package net.querz.mcaselector.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import net.querz.mcaselector.io.ImageHelper;
import org.controlsfx.control.RangeSlider;

public class HueRangeSlider extends RangeSlider {

	private BooleanProperty invertedProperty = new SimpleBooleanProperty(false);
	private final int width;

	public HueRangeSlider(float minHue, float maxHue, float low, float high, int width) {
		super(minHue, maxHue, minHue, maxHue);
		this.width = width;

		if (low > high) {
			invertedProperty.set(true);
			low = maxHue - low;
			high = maxHue - high;
		}

		setLowValue(low);
		setHighValue(high);

		getStyleClass().add("hue-range-slider");

		// create rainbow
		renderBackground();

		highValueProperty().addListener((v, o, n) -> renderBackground());
		lowValueProperty().addListener((v, o, n) -> renderBackground());

		invertedProperty.addListener((v, o, n) -> {
			double l = getLowValue();
			double h = getHighValue();
			if (n) {
				setHighValue(getMax() - l);
				setLowValue(getMax() - h);
			} else {
				setLowValue(getMax() - h);
				setHighValue(getMax() - l);
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
			return (float) getMax() - (float) getLowValue();
		}
		return (float) getLowValue();
	}

	public float getMaxHue() {
		if (invertedProperty.get()) {
			return (float) getMax() - (float) getHighValue();
		}
		return (float) getHighValue();
	}

	private void renderBackground() {
		Image bg = ImageHelper.renderGradient(width, 0, (float) getMax(), (float) getLowValue(), (float) getHighValue(), invertedProperty.get());
		BackgroundImage bgi = new BackgroundImage(bg, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
		setBackground(new Background(bgi));
	}
}
