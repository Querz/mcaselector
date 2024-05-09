package net.querz.mcaselector.ui.component;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
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
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.ui.UIFactory;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class OverlayBox extends BorderPane {

	private static final Image deleteIcon = FileHelper.getIconFromResources("img/delete");
	private static final Image flipIcon = FileHelper.getIconFromResources("img/flip");

	private static final PseudoClass invalid = PseudoClass.getPseudoClass("invalid");
	private static final PseudoClass selected = PseudoClass.getPseudoClass("selected");

	public DataProperty<Overlay> valueProperty;

	private BiConsumer<Overlay, Overlay> onTypeChange;
	private Consumer<Overlay> onValuesChange;
	private Consumer<Overlay> onDelete;

	private final GridPane inputs = new GridPane();
	private final GridPane options = new GridPane();

	private final ComboBox<OverlayType> type = new ComboBox<>();
	private final TextField minimum = new TextField();
	private final TextField maximum = new TextField();
	private final TextField additionalData = new TextField();
	private final Label gradient = new Label("");
	private final CheckBox active = new CheckBox();
	private final Label delete = new Label("", new ImageView(deleteIcon));

	public OverlayBox(Overlay value) {
		getStylesheets().add(OverlayBox.class.getClassLoader().getResource("style/component/overlay-box.css").toExternalForm());

		valueProperty = new DataProperty<>(value);

		getStyleClass().add("overlay-box");

		type.getItems().addAll(OverlayType.values());
		type.getSelectionModel().select(value.getType());
		type.setOnAction(e -> update(type.getSelectionModel().getSelectedItem()));

		minimum.textProperty().addListener((a, b, c) -> onMinimumInput(c));
		maximum.textProperty().addListener((a, b, c) -> onMaximumInput(c));
		minimum.setAlignment(Pos.CENTER);
		maximum.setAlignment(Pos.CENTER);
		minimum.setPromptText("<min>");
		maximum.setPromptText("<max>");

		inputs.getStyleClass().add("overlay-input-grid");

		inputs.add(type, 0, 0, 1, 1);
		inputs.add(minimum, 1, 0, 1, 1);
		inputs.add(maximum, 2, 0, 1, 1);

		setLeft(inputs);

		additionalData.setAlignment(Pos.CENTER);
		additionalData.textProperty().addListener((a, b, c) -> onAdditionalDataInput(c));
		setCenter(additionalData);

		options.getStyleClass().add("overlay-options-grid");

		active.selectedProperty().addListener((v, o, n) -> {
			getValue().setActive(n);
			if (onValuesChange != null) {
				onValuesChange.accept(getValue());
			}
		});
		active.setTooltip(UIFactory.tooltip(Translation.DIALOG_EDIT_OVERLAYS_OVERLAY_ACTIVE_TOOLTIP));

		gradient.getStyleClass().add("gradient-label");

		ContextMenu gradientMenu = new ContextMenu();
		gradientMenu.setOnHiding(e -> onValuesChange.accept(getValue()));
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

		hueSlider.lowProperty().addListener((v, o, n) -> {
			getValue().setMinHue(hueSlider.getMinHue());
			getValue().setMaxHue(hueSlider.getMaxHue());
			setGradientBackground(hueSlider);
		});

		hueSlider.highProperty().addListener((v, o, n) -> {
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
		delete.setOnMouseReleased(e -> onDelete.accept(getValue()));
		delete.setTooltip(UIFactory.tooltip(Translation.DIALOG_EDIT_OVERLAYS_DELETE_TOOLTIP));

		setRight(options);

		additionalData.setDisable(value.multiValues() == null);
		minimum.setText(value.getRawMin());
		maximum.setText(value.getRawMax());
		if (value.multiValues() != null) {
			additionalData.setText(value.getRawMultiValues());
		}
		active.setSelected(value.isActive());

		onMinimumInput(value.minString());
		onMaximumInput(value.maxString());
	}

	public void setSelected(boolean selected) {
		pseudoClassStateChanged(OverlayBox.selected, selected);
	}

	private Overlay getValue() {
		return valueProperty.get();
	}

	private void setGradientBackground(HueRangeSlider hueSlider) {
		float min = (float) hueSlider.getLow();
		float max = (float) hueSlider.getHigh();

		if (hueSlider.isInverted()) {
			min = 0.85f - max;
			max = 0.85f - (float) hueSlider.getLow();
		}

		gradient.setBackground(new Background((new BackgroundImage(ImageHelper.renderGradient(
			50,
			min, max, min, max,
			hueSlider.isInverted()),
			BackgroundRepeat.NO_REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT))));
	}

	public void setOnTypeChange(BiConsumer<Overlay, Overlay> consumer) {
		onTypeChange = consumer;
	}

	public void setOnValuesChange(Consumer<Overlay> consumer) {
		onValuesChange = consumer;
	}

	public void setOnDelete(Consumer<Overlay> consumer) {
		onDelete = consumer;
	}

	private void update(OverlayType type) {
		if (type == null || type == getValue().getType()) {
			return;
		}

		// create new data parser and fill with min / max values
		Overlay oldValue = getValue();
		Overlay newValue = type.instance();

		newValue.setActive(getValue().isActive());

		valueProperty.set(newValue);

		// initialize new value and show parsing errors in ui
		onMinimumInput(minimum.getText());
		onMaximumInput(maximum.getText());

		additionalData.setDisable(newValue.multiValues() == null);

		active.setSelected(newValue.isActive());

		onTypeChange.accept(oldValue, getValue());
	}

	private void onMinimumInput(String newValue) {
		displayValid(getValue().setMin(newValue));
		if (onValuesChange != null) {
			onValuesChange.accept(getValue());
		}
	}

	private void onMaximumInput(String newValue) {
		displayValid(getValue().setMax(newValue));
		if (onValuesChange != null) {
			onValuesChange.accept(getValue());
		}
	}

	private void onAdditionalDataInput(String newValue) {
		displayValid(getValue().setMultiValuesString(newValue));
		if (onValuesChange != null) {
			onValuesChange.accept(getValue());
		}
	}

	private void displayValid(boolean valid) {
		pseudoClassStateChanged(invalid, !valid);
	}
}
