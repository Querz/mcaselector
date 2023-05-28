package net.querz.mcaselector.ui;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ScrollEvent;
import net.querz.mcaselector.text.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public final class UIFactory {

	private static final Logger LOGGER = LogManager.getLogger(UIFactory.class);

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
			if (!n.matches("-?\\d*")) {
				sliderValue.setText(n.replaceAll("[^\\-\\d]", ""));
			} else if ("".equals(n)) {
				slider.setValue(slider.getMin());
			} else {
				try {
					slider.setValue(Integer.parseInt(n));
				} catch (NumberFormatException ex) {
					slider.setValue(slider.getMin());
				}
			}
		});
		sliderValue.focusedProperty().addListener((l, o, n) -> {
			if (!n) {
				sliderValue.setText((int) Math.round(slider.getValue()) + "");
			}
		});
		slider.valueProperty().addListener((l, o, n) -> {
			if (n.intValue() != slider.getMin() || slider.isFocused()) {
				sliderValue.setText((int) Math.round(slider.getValue()) + "");
			}
		});

		EventHandler<? super ScrollEvent> scrollEvent = e -> {
			if (e.getDeltaY() > 0) {
				slider.setValue(slider.getValue() + 1);
			} else if (e.getDeltaY() < 0) {
				slider.setValue(slider.getValue() - 1);
			}
		};

		sliderValue.setOnScroll(scrollEvent);
		slider.setOnScroll(scrollEvent);

		sliderValue.setText((int) Math.round(slider.getValue()) + "");
		return sliderValue;
	}

	public static Hyperlink hyperlink(String text, String url, Node graphic) {
		Hyperlink hyperlink;
		if (graphic == null) {
			hyperlink = new Hyperlink(text);
		} else {
			hyperlink = new Hyperlink(text, graphic);
		}
		hyperlink.setOnAction(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URL(url).toURI());
				} catch (IOException | URISyntaxException ex) {
					LOGGER.warn("cannot open url using a default browser", ex);
				}
			}
		});
		return hyperlink;
	}

	public static Hyperlink explorerLink(Translation text, File file, Node graphic) {
		Hyperlink hyperlink;
		if (graphic == null) {
			hyperlink = new Hyperlink();
		} else {
			hyperlink = new Hyperlink("", graphic);
		}
		hyperlink.textProperty().bind(text.getProperty());

		hyperlink.setOnAction(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException ex) {
					LOGGER.warn("cannot open file or directory", ex);
				}
			}
		});
		return hyperlink;
	}
}
