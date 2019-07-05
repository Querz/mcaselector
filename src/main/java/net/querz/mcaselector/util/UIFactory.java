package net.querz.mcaselector.util;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;

public final class UIFactory {

	private UIFactory() {}

	public static Menu menu(Translation translation) {
		Menu menu = new Menu();
		menu.textProperty().bind(translation.getProperty());
		return menu;
	}

	public static MenuItem menuItem(Translation translation) {
		MenuItem item = new Menu();
		item.textProperty().bind(translation.getProperty());
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
}
