package net.querz.mcaselector.ui;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import net.querz.mcaselector.text.Translation;

public final class UIFactory {

	private UIFactory() {}

	public static Menu menu(Translation translation) {
		Menu menu = new Menu();
		menu.textProperty().bind(translation.getProperty());
		return menu;
	}

	public static MenuItem menuItem(Translation translation) {
		MenuItem item = new MenuItem();
		item.textProperty().bind(translation.getProperty());
		item.setMnemonicParsing(false);
		return item;
	}

	public static CheckMenuItem checkMenuItem(Translation translation, boolean selected) {
		CheckMenuItem item = new CheckMenuItem();
		item.textProperty().bind(translation.getProperty());
		item.setSelected(selected);
		return item;
	}

	public static Label label(Translation translation) {
		Label label = new Label();
		label.textProperty().bind(translation.getProperty());
		return label;
	}

	public static SeparatorMenuItem separator() {
		return new SeparatorMenuItem();
	}

	public static Button button(Translation translation) {
		Button button = new Button();
		button.textProperty().bind(translation.getProperty());
		return button;
	}

	public static Tooltip tooltip(Translation translation) {
		Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(translation.getProperty());
		return tooltip;
	}

	public static CheckBox checkbox(Translation translation) {
		CheckBox checkbox = new CheckBox();
		checkbox.textProperty().bind(translation.getProperty());
		return checkbox;
	}

	public static RadioButton radio(Translation translation) {
		RadioButton radio = new RadioButton();
		radio.textProperty().bind(translation.getProperty());
		return radio;
	}

	public static TextField attachTextFieldToSlider(Slider slider) {
		TextField sliderValue = new TextField();
		sliderValue.getStyleClass().add("slider-value-field");
		sliderValue.textProperty().addListener((l, o, n) -> {
			if (!n.matches("\\d*")) {
				sliderValue.setText(n.replaceAll("[^\\d]", ""));
			} else if ("".equals(n)) {
				slider.setValue(slider.getMin());
			} else {
				slider.setValue(Integer.parseInt(n));
			}
		});
		sliderValue.focusedProperty().addListener((l, o, n) -> {
			if (!n) {
				sliderValue.setText((int) slider.getValue() + "");
			}
		});
		slider.valueProperty().addListener((l, o, n) -> {
			if (n.intValue() != slider.getMin() || slider.isFocused()) {
				sliderValue.setText(n.intValue() + "");
			}
		});
		sliderValue.setText((int) slider.getValue() + "");
		return sliderValue;
	}
}
