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
import net.querz.mcaselector.tiles.overlay.OverlayDataParser;
import net.querz.mcaselector.tiles.overlay.OverlayType;

public class OverlayBox extends BorderPane {

	private static final Image deleteIcon = FileHelper.getIconFromResources("img/delete");

	private OverlayDataParser value;

	private final GridPane inputs = new GridPane();
	private final GridPane options = new GridPane();

	private final ComboBox<OverlayType> type = new ComboBox<>();
	private final TextField minimum = new TextField();
	private final TextField maximum = new TextField();
	private final TextField additionalData = new TextField();
	private final CheckBox active = new CheckBox();
	private final Label delete = new Label("", new ImageView(deleteIcon));

	public OverlayBox(OverlayDataParser value) {
		this.value = value;

		getStyleClass().add("overlay-box");

		type.getItems().addAll(OverlayType.values());
		type.getSelectionModel().select(value.getType());

		minimum.textProperty().addListener((a, b, c) -> onMinimumInput(value, c));
		maximum.textProperty().addListener((a, b, c) -> onMaximumInput(value, c));
		minimum.setAlignment(Pos.CENTER);
		maximum.setAlignment(Pos.CENTER);

		additionalData.setDisable(value.multiValues() == null);

		additionalData.getStyleClass().add("additional-data");

		active.setSelected(value.isActive());

		inputs.getStyleClass().add("overlay-input-grid");

		inputs.add(type, 0, 0, 1, 1);
		inputs.add(minimum, 1, 0, 1, 1);
		inputs.add(maximum, 2, 0, 1, 1);

		setLeft(inputs);

		additionalData.setAlignment(Pos.CENTER);
		setCenter(additionalData);

		options.getStyleClass().add("overlay-options-grid");

		options.add(active, 0, 0, 1, 1);
		options.add(delete, 1, 0, 1, 1);

		setRight(options);
	}

	private void onMinimumInput(OverlayDataParser parser, String newValue) {
		System.out.println("parsing minimum input");
		if (parser.setMin(newValue)) {
			getStyleClass().remove("overlay-box-invalid");
		} else {
			if (!getStyleClass().contains("overlay-box-invalid")) {
				getStyleClass().add("overlay-box-invalid");
			}
		}
		// TODO: update
	}

	private void onMaximumInput(OverlayDataParser parser, String newValue) {
		System.out.println("parsing maximum input");
		if (parser.setMax(newValue)) {
			getStyleClass().remove("overlay-box-invalid");
		} else {
			if (!getStyleClass().contains("overlay-box-invalid")) {
				getStyleClass().add("overlay-box-invalid");
			}
		}
		// TODO: update
	}
}
