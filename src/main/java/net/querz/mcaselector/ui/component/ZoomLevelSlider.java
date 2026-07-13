package net.querz.mcaselector.ui.component;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.scene.control.Slider;
import javafx.util.StringConverter;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.util.math.Bits;

public class ZoomLevelSlider extends Slider {

    private static final int[] values = new int[Bits.msbPosition(Config.MAX_ZOOM_LEVEL) + 3]; // include 0 and the max zoom level
    static {
        for (int i = 0; i < values.length - 1; i++) {
            values[i + 1] = (int) Math.pow(2, i);
        }
    }

	private final SimpleIntegerProperty zoomLevelValue = new SimpleIntegerProperty();

    public ZoomLevelSlider(int init) {
        super(0, values.length - 1, Bits.msbPosition(init) + 1);
		zoomLevelValue.set(init);

        setMajorTickUnit(1);
        setMinorTickCount(0);
        setSnapToTicks(true);
        setShowTickMarks(true);
        setShowTickLabels(true);

        setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double index) {
                return "" + values[index.intValue()];
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });

        valueProperty().addListener((o, v, n) -> zoomLevelValue.set(values[n.intValue()]));
    }

	public void setZoomLevelValue(int zoomLevelValue) {
		this.zoomLevelValue.set(zoomLevelValue);
	}

	public int getZoomLevelValue() {
		return this.zoomLevelValue.get();
	}

	public ObservableIntegerValue getZoomLevelValueProperty() {
		return zoomLevelValue;
	}
}
