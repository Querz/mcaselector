package net.querz.mcaselector.ui.component.filter;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;

public class TextFilterBox extends FilterBox {

	protected final TextField input = new TextField();
	private final ComboBox<Comparator> comparator = new ComboBox<>();

	private static final PseudoClass invalid = PseudoClass.getPseudoClass("invalid");
	private static final String stylesheet = TextFilterBox.class.getClassLoader().getResource("style/component/text-filter-box.css").toExternalForm();

	public TextFilterBox(FilterBox parent, TextFilter<?> filter, boolean root) {
		super(parent, filter, root);
		getStyleClass().add("text-filter-box");
		input.setPromptText(filter.getFormatText());
		input.textProperty().addListener((a, b, c) -> onTextInput(filter, c));
		input.setAlignment(Pos.CENTER);

		comparator.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_COMPARATOR_TOOLTIP));
		comparator.getItems().addAll(filter.getComparators());
		comparator.getSelectionModel().select(filter.getComparator());
		comparator.setOnAction(e -> onComparator(filter));

		comparator.getStyleClass().add("filter-comparator-combo-box");

		filterOperators.add(comparator, 2, 0, 1, 1);

		setCenter(input);
		setText(filter.getRawValue());
		onTextInput(filter, filter.getRawValue());

		getStylesheets().add(stylesheet);
	}

	public void setText(String text) {
		input.setText(text);
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
