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
		input.setPromptText(filter.getType().toString());
		input.setText(filter.getFilterValue().toString());
		input.textProperty().addListener((a, b, c) -> onTextInput(filter, b, c));
		input.setAlignment(Pos.TOP_CENTER);

		comparator.getItems().addAll(filter.getComparators());
		comparator.getSelectionModel().select(filter.getComparator());
		comparator.setOnAction(e -> onComparator(filter));

		filterOperators.add(comparator, 2, 0, 1, 1);

		setCenter(input);
	}

	private void onTextInput(Filter filter, String oldValue, String newValue) {
		if (!filter.setFilterValue(newValue)) {
			input.setText(oldValue);
		}
	}

	private void onComparator(NumberFilter filter) {
		filter.setComparator(comparator.getSelectionModel().getSelectedItem());
	}
}
