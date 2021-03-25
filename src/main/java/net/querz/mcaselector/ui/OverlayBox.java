package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import net.querz.mcaselector.tiles.overlay.OverlayType;

import java.awt.Color;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class OverlayBox extends BorderPane {

	private static final Image deleteIcon = FileHelper.getIconFromResources("img/delete");
	private static final Image flipIcon = FileHelper.getIconFromResources("img/flip");

	private OverlayParser value;

	private BiConsumer<OverlayParser, OverlayParser> onTypeChange;
	private Consumer<OverlayParser> onDelete;

	private final GridPane inputs = new GridPane();
	private final GridPane options = new GridPane();

	private final ComboBox<OverlayType> type = new ComboBox<>();
	private final TextField minimum = new TextField();
	private final TextField maximum = new TextField();
	private final TextField additionalData = new TextField();
	private final Label gradient = new Label("");
	private final CheckBox active = new CheckBox();
	private final Label delete = new Label("", new ImageView(deleteIcon));

	public OverlayBox(OverlayParser value) {
		this.value = value;

		getStyleClass().add("overlay-box");

		type.getItems().addAll(OverlayType.values());
		type.getSelectionModel().select(value.getType());
		type.setOnAction(e -> update(type.getSelectionModel().getSelectedItem()));

		minimum.textProperty().addListener((a, b, c) -> onMinimumInput(c));
		maximum.textProperty().addListener((a, b, c) -> onMaximumInput(c));
		minimum.setAlignment(Pos.CENTER);
		maximum.setAlignment(Pos.CENTER);

		inputs.getStyleClass().add("overlay-input-grid");

		inputs.add(type, 0, 0, 1, 1);
		inputs.add(minimum, 1, 0, 1, 1);
		inputs.add(maximum, 2, 0, 1, 1);

		setLeft(inputs);

		additionalData.setAlignment(Pos.CENTER);
		setCenter(additionalData);

		options.getStyleClass().add("overlay-options-grid");

		active.selectedProperty().addListener((v, o, n) -> getValue().setActive(n));

		gradient.getStyleClass().add("gradient-label");

		ContextMenu gradientMenu = new ContextMenu();
		HueRangeSlider hueSlider = new HueRangeSlider(0, 0.85f, value.getMinHue(), value.getMaxHue(), 400);
		setGradientBackground(hueSlider);
		Label flip = new Label("", new ImageView(flipIcon));
		flip.getStyleClass().add("flip-label");
		flip.setOnMouseReleased(a -> {
			hueSlider.setInverted(!hueSlider.isInverted());
			getValue().setMinHue(hueSlider.getMinHue());
			getValue().setMaxHue(hueSlider.getMaxHue());
			setGradientBackground(hueSlider);
		});

		hueSlider.lowValueProperty().addListener((v, o, n) -> {
			getValue().setMinHue(hueSlider.getMinHue());
			getValue().setMaxHue(hueSlider.getMaxHue());
			setGradientBackground(hueSlider);
		});

		hueSlider.highValueProperty().addListener((v, o, n) -> {
			getValue().setMinHue(hueSlider.getMinHue());
			getValue().setMaxHue(hueSlider.getMaxHue());
			setGradientBackground(hueSlider);
		});

		HBox slider = new HBox();
		slider.getStyleClass().add("slider-box");
		slider.setAlignment(Pos.CENTER_LEFT);
		slider.getChildren().addAll(hueSlider, flip);

		CustomMenuItem menuItem = new CustomMenuItem(slider, false);
		menuItem.getStyleClass().add("custom-menu-item");
		gradientMenu.getItems().add(menuItem);

		gradient.setOnMouseClicked(e -> {
			Bounds screenBounds = gradient.localToScreen(gradient.getBoundsInLocal());
			gradientMenu.show(gradient, screenBounds.getMinX(), screenBounds.getMinY());
			Platform.runLater(() -> {
				gradientMenu.setX((screenBounds.getMinX() + screenBounds.getMaxX()) / 2 - (gradientMenu.getWidth() / 2));
				gradientMenu.setY((screenBounds.getMinY() + screenBounds.getMaxY()) / 2 - (gradientMenu.getHeight() / 2));
			});
		});

		options.add(gradient, 0, 0, 1, 1);
		options.add(active, 1, 0, 1, 1);
		options.add(delete, 2, 0, 1, 1);

		delete.getStyleClass().add("control-label");
		delete.setOnMouseReleased(e -> onDelete.accept(value));

		setRight(options);

		additionalData.setDisable(value.multiValues() == null);
		minimum.setText(value.getRawMin());
		maximum.setText(value.getRawMax());
		active.setSelected(value.isActive());

		onMinimumInput(value.minString());
		onMaximumInput(value.maxString());
	}

	private OverlayParser getValue() {
		return value;
	}

	private void setGradientBackground(HueRangeSlider hueSlider) {
		float min = (float) hueSlider.getLowValue();
		float max = (float) hueSlider.getHighValue();

		if (hueSlider.isInverted()) {
			min = 0.85f - max;
			max = 0.85f - (float) hueSlider.getLowValue();
		}

		gradient.setBackground(new Background((new BackgroundImage(ImageHelper.renderGradient(
			50,
			min, max, min, max,
			hueSlider.isInverted()),
			BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT))));
	}

	public void setOnTypeChange(BiConsumer<OverlayParser, OverlayParser> consumer) {
		onTypeChange = consumer;
	}

	public void setOnDelete(Consumer<OverlayParser> consumer) {
		onDelete = consumer;
	}

	private void update(OverlayType type) {
		if (type == null || type == value.getType()) {
			return;
		}

		// create new data parser and fill with min / max values
		OverlayParser oldValue = value;
		OverlayParser newValue = type.instance();

		newValue.setActive(value.isActive());

		value = newValue;

		// initialize new value and show parsing errors in ui
		onMinimumInput(minimum.getText());
		onMaximumInput(maximum.getText());

		additionalData.setDisable(newValue.multiValues() == null);

		active.setSelected(newValue.isActive());

		onTypeChange.accept(oldValue, newValue);
	}

	private void onMinimumInput(String newValue) {
		displayValid(value.setMin(newValue));
	}

	private void onMaximumInput(String newValue) {
		displayValid(value.setMax(newValue));
	}

	private void displayValid(boolean valid) {
		if (valid) {
			getStyleClass().remove("overlay-box-invalid");
			active.setDisable(false);
		} else if (!getStyleClass().contains("overlay-box-invalid")) {
			getStyleClass().add("overlay-box-invalid");
			active.setDisable(true);
		}
	}
}
