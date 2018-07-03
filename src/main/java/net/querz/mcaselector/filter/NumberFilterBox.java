package net.querz.mcaselector.filter;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import net.querz.mcaselector.filter.structure.Comparator;
import net.querz.mcaselector.filter.structure.Filter;
import net.querz.mcaselector.filter.structure.NumberFilter;

public class NumberFilterBox extends FilterBox {

	private TextField input = new TextField();
	private ComboBox<Comparator> comparator = new ComboBox<>();

	public NumberFilterBox(FilterBox parent, NumberFilter filter, boolean root) {
		super(parent, filter, root);
		getStyleClass().add("number-filter-box");
		input.setPromptText(filter.getFormatText());
		input.setText(filter.getFilterValue().toString());
		input.textProperty().addListener((a, b, c) -> onTextInput(filter, b, c));
		input.setAlignment(Pos.TOP_CENTER);

		comparator.getItems().addAll(filter.getComparators());
		comparator.getSelectionModel().select(filter.getComparator());
		comparator.setOnAction(e -> onComparator(filter));

		comparator.getStyleClass().add("filter-comparator-combo-box");

		filterOperators.add(comparator, 2, 0, 1, 1);

		setCenter(input);
	}

	public void setText(String text) {
		input.setText(text);
	}

	private void onTextInput(Filter filter, String oldValue, String newValue) {
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
