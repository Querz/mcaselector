package net.querz.mcaselector.ui.component;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import net.querz.mcaselector.point.Point2f;

public class RangeSlider extends Pane {

	private final Pane lowerThumb = new Pane();
	private final Pane upperThumb = new Pane();
	private final Pane range = new Pane();

	private final DoubleProperty minProperty = new SimpleDoubleProperty();
	private final DoubleProperty maxProperty = new SimpleDoubleProperty();
	private final DoubleProperty lowProperty = new SimpleDoubleProperty();
	private final DoubleProperty highProperty = new SimpleDoubleProperty();

	public RangeSlider(double min, double max, double low, double high) {
		if (min < 0) {
			setMin(min);
			setMax(max);
		} else {
			setMax(max);
			setMin(min);
		}
		if (low < 0) {
			setLow(low);
			setHigh(high);
		} else {
			setHigh(high);
			setLow(low);
		}

		setOnMousePressed(this::onMousePressed);
		setOnMouseReleased(this::onMouseReleased);
		setOnMouseDragged(this::onMouseDragged);
		widthProperty().addListener(n -> update());

		minProperty.addListener((v, o, n) -> update());
		maxProperty.addListener((v, o, n) -> update());
		lowProperty.addListener((v, o, n) -> update());
		highProperty.addListener((v, o, n) -> update());

		lowerThumb.setOnMousePressed(e -> lowerThumb.toFront());
		upperThumb.setOnMousePressed(e -> upperThumb.toFront());

		range.setId("range-bar");
		lowerThumb.setId("lower-thumb");
		upperThumb.setId("upper-thumb");

		getChildren().addAll(range, lowerThumb, upperThumb);

		Platform.runLater(this::update);
	}

	private void update() {
		// size of range
		double lowPercent = (getLow() - getMin()) / (getMax() - getMin());
		double highPercent = (getHigh() - getMin()) / (getMax() - getMin());
		range.setLayoutX((getWidth() * lowPercent));
		range.setPrefWidth(getWidth() * (highPercent - lowPercent));

		// position of lowerThumb and upperThumb
		lowerThumb.setLayoutX(getWidth() * lowPercent - lowerThumb.getWidth() / 2);
		upperThumb.setLayoutX(getWidth() * highPercent - upperThumb.getWidth() / 2);
	}

	private Point2f previousMouseLocation;
	private Pane pressedPane;

	private void onMouseDragged(MouseEvent e) {
		double x = Math.max(Math.min(e.getX(), getWidth() + upperThumb.getWidth() / 2), -lowerThumb.getWidth() / 2);

		Point2f mouseLocation = new Point2f(x, e.getY());

		if (pressedPane == lowerThumb) {
			if (x > upperThumb.getLayoutX()) {
				setLow(getHigh());
				update();
				return;
			}
			double offset = x - previousMouseLocation.getX();
			double rangeOffset = offset * ((getMax() - getMin()) / getWidth());
			setLow(getLow() + rangeOffset);
			update();
		} else if (pressedPane == upperThumb) {
			if (x < lowerThumb.getLayoutX() + lowerThumb.getWidth()) {
				setHigh(getLow());
				update();
				return;
			}
			double offset = x - previousMouseLocation.getX();
			double rangeOffset = offset * ((getMax() - getMin()) / getWidth());
			setHigh(getHigh() + rangeOffset);
			update();
		} else if (pressedPane == range) {
			double offset = x - previousMouseLocation.getX();
			double rangeOffset = offset * ((getMax() - getMin()) / getWidth());

			// check if this would move the range below min
			if (getLow() + rangeOffset < getMin()) {
				// calculate offset to 0
				rangeOffset = getMin() - getLow();
			}

			// check if this would move the range above max
			if (getHigh() + rangeOffset > getMax()) {
				// calculate offset to max
				rangeOffset = getMax() - getHigh();
			}

			setLow(getLow() + rangeOffset);
			setHigh(getHigh() + rangeOffset);
			update();
		}

		previousMouseLocation = mouseLocation;
	}

	private void onMousePressed(MouseEvent e) {
		previousMouseLocation = new Point2f(e.getX(), e.getY());
		pressedPane = (Pane) e.getTarget();
	}

	private void onMouseReleased(MouseEvent e) {
		previousMouseLocation = null;
		pressedPane = null;
	}

	public DoubleProperty minProperty() {
		return minProperty;
	}

	public DoubleProperty maxProperty() {
		return maxProperty;
	}

	public DoubleProperty lowProperty() {
		return lowProperty;
	}

	public DoubleProperty highProperty() {
		return highProperty;
	}

	public double getMin() {
		return minProperty.get();
	}

	public double getMax() {
		return maxProperty.get();
	}

	public double getLow() {
		return lowProperty.get();
	}

	public double getHigh() {
		return highProperty.get();
	}

	public void setMin(double min) {
		minProperty.set(Math.min(min, getMax()));
	}

	public void setMax(double max) {
		maxProperty.set(Math.max(max, getMin()));
	}

	public void setLow(double low) {
		lowProperty.set(low > getHigh() ? getHigh() : Math.max(low, getMin()));
	}

	public void setHigh(double high) {
		highProperty.set(high < getLow() ? getLow() : (Math.min(high, getMax())));
	}
}
