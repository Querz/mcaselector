package net.querz.mcaselector.ui;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.NumberFilter;
import net.querz.mcaselector.util.Translation;
import net.querz.mcaselector.util.UIFactory;

public class NumberFilterBox extends FilterBox {

	private TextField input = new TextField();
	private ComboBox<Comparator> comparator = new ComboBox<>();

	public NumberFilterBox(FilterBox parent, NumberFilter filter, boolean root) {
		super(parent, filter, root);
		getStyleClass().add("number-filter-box");
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
	}

	public void setText(String text) {
		input.setText(text);
	}

	private void onTextInput(Filter filter, String newValue) {
		filter.setFilterValue(newValue);
		if (!filter.isValid()) {
			if (!getStyleClass().contains("number-filter-box-invalid")) {
				getStyleClass().add("number-filter-box-invalid");
			}
		} else {
			getStyleClass().remove("number-filter-box-invalid");
		}
		callUpdateEvent();
	}

	private void onComparator(NumberFilter filter) {
		filter.setComparator(comparator.getSelectionModel().getSelectedItem());
		callUpdateEvent();
	}
}
