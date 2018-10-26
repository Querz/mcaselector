package net.querz.mcaselector.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.querz.mcaselector.util.Helper;
import java.util.Optional;

public class ColorPicker extends TransparentStage {

	private Slider red = createSlider(0, 255, 1, 0);
	private Slider green = createSlider(0, 255, 1, 0);
	private Slider blue = createSlider(0, 255, 1, 0);
	private Slider opacity = createSlider(0, 255, 1, 0);
	private StackPane preview = new StackPane();
	private Button ok = new Button("OK");
	private Button cancel = new Button("Cancel");
	private Color result;

	public ColorPicker(Stage stage, Color color) {
		super(stage);
		cancel.setOnAction(e -> {
			result = null;
			close();
		});
		ok.setOnAction(e -> close());

		red.setValue(color.getRed() * 255);
		green.setValue(color.getGreen() * 255);
		blue.setValue(color.getBlue() * 255);
		opacity.setValue(color.getOpacity() * 255);

		red.valueProperty().addListener((l, o, n) -> onValueChange());
		green.valueProperty().addListener((l, o, n) -> onValueChange());
		blue.valueProperty().addListener((l, o, n) -> onValueChange());
		opacity.valueProperty().addListener((l, o, n) -> onValueChange());

		preview.getStyleClass().add("color-picker-preview");

		GridPane grid = new GridPane();
		grid.getStyleClass().add("color-picker-sliders");
		grid.add(label("R", "color-picker-slider-label"), 0, 0, 1, 1);
		grid.add(label("G", "color-picker-slider-label"), 0, 1, 1, 1);
		grid.add(label("B", "color-picker-slider-label"), 0, 2, 1, 1);
		grid.add(label("A", "color-picker-slider-label"), 0, 3, 1, 1);
		grid.add(red, 1, 0, 1, 1);
		grid.add(green, 1, 1, 1, 1);
		grid.add(blue, 1, 2, 1, 1);
		grid.add(opacity, 1, 3, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(red), 2, 0, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(green), 2, 1, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(blue), 2, 2, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(opacity), 2, 3, 1, 1);

		HBox buttonBox = new HBox();
		buttonBox.getChildren().addAll(cancel, ok);
		buttonBox.getStyleClass().add("color-picker-button-box");

		BorderPane borderPane = new BorderPane();
		borderPane.getStyleClass().add("color-picker");
		borderPane.setTop(grid);
		borderPane.setCenter(preview);
		borderPane.setBottom(buttonBox);
		borderPane.setLeft(borderElement("border-element-vertical"));
		borderPane.setRight(borderElement("border-element-vertical"));

		setContent(borderPane);
	}

	public Optional<Color> showColorPicker() {
		showAndWait();
		return Optional.ofNullable(result);
	}

	private Label label(String name, String styleClass) {
		Label label = new Label(name);
		label.getStyleClass().add(styleClass);
		return label;
	}

	private Region borderElement(String styleClass) {
		Region border = new Region();
		border.getStyleClass().add(styleClass);
		return border;
	}

	private void onValueChange() {
		result = new Color(red.getValue() / 255, green.getValue() / 255, blue.getValue() / 255, opacity.getValue() / 255);
		preview.setBackground(new Background(new BackgroundFill(result, CornerRadii.EMPTY, Insets.EMPTY)));
	}

	private Slider createSlider(int min, int max, int steps, int init) {
		Slider slider = new Slider(min, max, init);
		slider.setMajorTickUnit(steps);
		slider.setMinorTickCount(0);
		slider.setBlockIncrement(steps);
		return slider;
	}
}