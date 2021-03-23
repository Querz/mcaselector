package net.querz.mcaselector.ui;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import net.querz.mcaselector.tiles.overlay.OverlayType;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class OverlayBox extends BorderPane {

	private static final Image deleteIcon = FileHelper.getIconFromResources("img/delete");

	private OverlayParser value;

	private BiConsumer<OverlayParser, OverlayParser> onTypeChange;
	private Consumer<OverlayParser> onDelete;

	private final GridPane inputs = new GridPane();
	private final GridPane options = new GridPane();

	private final ComboBox<OverlayType> type = new ComboBox<>();
	private final TextField minimum = new TextField();
	private final TextField maximum = new TextField();
	private final TextField additionalData = new TextField();
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

		active.selectedProperty().addListener((v, o, n) -> value.setActive(n));

		options.add(active, 0, 0, 1, 1);
		options.add(delete, 1, 0, 1, 1);

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
