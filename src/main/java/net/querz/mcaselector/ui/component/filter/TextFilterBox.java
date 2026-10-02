package net.querz.mcaselector.ui.component.filter;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TextFilterBox extends FilterBox {

	protected TextInputControl input;
	private final ComboBox<Comparator> comparator = new ComboBox<>();

	private static final PseudoClass invalid = PseudoClass.getPseudoClass("invalid");
	private static final String stylesheet = Objects.requireNonNull(TextFilterBox.class.getClassLoader().getResource("style/component/text-filter-box.css")).toExternalForm();

	public TextFilterBox(FilterBox parent, TextFilter<?> filter, boolean root) {
		super(parent, filter, root);
		getStyleClass().add("text-filter-box");
		ComboBox<String> knownValuePicker = createKnownValuePicker(filter);
		if (knownValuePicker == null) {
			input = new javafx.scene.control.TextField();
			setCenter(input);
		} else {
			input = knownValuePicker.getEditor();
			setCenter(knownValuePicker);
		}
		input.setPromptText(filter.getFormatText());
		input.textProperty().addListener((a, b, c) -> onTextInput(filter, c));
		if (input instanceof javafx.scene.control.TextField textField) {
			textField.setAlignment(Pos.CENTER);
		}

		comparator.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_COMPARATOR_TOOLTIP));
		comparator.getItems().addAll(filter.getComparators());
		comparator.getSelectionModel().select(filter.getComparator());
		comparator.setOnAction(e -> onComparator(filter));

		comparator.getStyleClass().add("filter-comparator-combo-box");

		filterOperators.add(comparator, 2, 0, 1, 1);

		setText(filter.getRawValue());
		onTextInput(filter, filter.getRawValue());

		getStylesheets().add(stylesheet);
	}

	public void setText(String text) {
		input.setText(text);
	}

	private ComboBox<String> createKnownValuePicker(TextFilter<?> filter) {
		List<String> values = filter.getKnownValues();
		if (values == null) {
			return null;
		}
		ComboBox<String> knownValues = new FilterValuePicker();
		knownValues.getItems().addAll(values);

		knownValues.setEditable(true);
		knownValues.setMaxWidth(Double.MAX_VALUE);
		knownValues.getStyleClass().add("filter-known-value-combo-box");
		knownValues.skinProperty().addListener((observable, oldSkin, skin) -> {
			if (skin instanceof ComboBoxListViewSkin<?> comboSkin) {
				((Region) comboSkin.getPopupContent()).setPrefHeight(200);
				comboSkin.getPopupContent().addEventFilter(KeyEvent.ANY,
						event -> handleSpace(event, knownValues.getEditor()));
			}
		});
		knownValues.addEventFilter(KeyEvent.KEY_PRESSED, event -> handleSpace(event, knownValues.getEditor()));
		knownValues.addEventFilter(KeyEvent.KEY_RELEASED, event -> handleSpace(event, knownValues.getEditor()));
		knownValues.getEditor().addEventFilter(KeyEvent.KEY_TYPED, event -> handleSpace(event, knownValues.getEditor()));
		List<String> allKnownValues = List.copyOf(knownValues.getItems());
		boolean[] updatingItems = {false};
		knownValues.valueProperty().addListener((observable, oldValue, selectedValue) -> {
			if (updatingItems[0] || selectedValue == null || !allKnownValues.contains(selectedValue)) {
				return;
			}
			String text = knownValues.getEditor().getText();
			int comma = text.lastIndexOf(',');
			if (comma < 0) {
				Platform.runLater(() -> {
					if (Objects.equals(knownValues.getEditor().getText(), selectedValue)) {
						knownValues.getEditor().positionCaret(selectedValue.length());
					}
				});
				return;
			}
			String completed = text.substring(0, comma + 1) + selectedValue;
			Platform.runLater(() -> {
				if (!Objects.equals(knownValues.getEditor().getText(), selectedValue)) {
					return;
				}
				updatingItems[0] = true;
				try {
					knownValues.getItems().setAll(getSuggestions(allKnownValues, completed));
					knownValues.getEditor().setText(completed);
					knownValues.getEditor().positionCaret(completed.length());
				} finally {
					updatingItems[0] = false;
				}
				knownValues.hide();
			});
		});
		knownValues.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
			if (updatingItems[0]) {
				return;
			}
			// Let ComboBox finish committing a selection before filtering its items.
			Platform.runLater(() -> {
				if (!Objects.equals(newValue, knownValues.getEditor().getText())
						|| Objects.equals(newValue, knownValues.getSelectionModel().getSelectedItem())) {
					return;
				}
				int anchor = knownValues.getEditor().getAnchor();
				int caret = knownValues.getEditor().getCaretPosition();
				updatingItems[0] = true;
				try {
					if (!knownValues.getSelectionModel().isEmpty()) {
						knownValues.getSelectionModel().clearSelection();
					}
					knownValues.getItems().setAll(getSuggestions(allKnownValues, newValue));
					knownValues.getEditor().setText(newValue);
					knownValues.getEditor().selectRange(anchor, caret);
				} finally {
					updatingItems[0] = false;
				}
				if (knownValues.getItems().isEmpty()) {
					knownValues.hide();
				} else if (knownValues.getEditor().isFocused() && newValue != null && !newValue.isEmpty()) {
					knownValues.show();
				}
			});
		});
		return knownValues;
	}

	private void handleSpace(KeyEvent event, TextInputControl editor) {
		if (event.getEventType() == KeyEvent.KEY_TYPED && " ".equals(event.getCharacter())) {
			if (!event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
				editor.replaceSelection(" ");
			}
			event.consume();
		} else if (event.getCode() == KeyCode.SPACE) {
			event.consume();
		}
	}

	private List<String> getSuggestions(List<String> values, String text) {
		String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
		String prefix = normalized.substring(normalized.lastIndexOf(',') + 1).trim();
		List<String> enteredValues = Arrays.stream(normalized.split(","))
				.map(String::trim)
				.toList();
		return values.stream()
				.filter(value -> matchesPrefix(value, prefix))
				.filter(value -> !enteredValues.contains(value.toLowerCase(Locale.ROOT)))
				.toList();
	}

	private boolean matchesPrefix(String value, String prefix) {
		String normalized = value.toLowerCase(Locale.ROOT);
		return normalized.startsWith(prefix) || normalized.startsWith("minecraft:") && normalized.substring(10).startsWith(prefix);
	}

	private void onTextInput(Filter<?> filter, String newValue) {
		filter.setFilterValue(newValue);
		pseudoClassStateChanged(invalid, !filter.isValid());
		callUpdateEvent();
	}

	private void onComparator(TextFilter<?> filter) {
		filter.setComparator(comparator.getSelectionModel().getSelectedItem());
		callUpdateEvent();
	}
}
