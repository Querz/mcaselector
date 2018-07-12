package net.querz.mcaselector.ui;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.TextFilter;

public class TextFilterBox extends FilterBox {

	private TextField input = new TextField();
	private ComboBox<Comparator> comparator = new ComboBox<>();

	public TextFilterBox(FilterBox parent, TextFilter filter, boolean root) {
		super(parent, filter, root);
		getStyleClass().add("text-filter-box");
		input.setPromptText(filter.getFormatText());
		input.textProperty().addListener((a, b, c) -> onTextInput(filter, c));
		input.setAlignment(Pos.TOP_CENTER);

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
			if (!getStyleClass().contains("text-filter-box-invalid")) {
				getStyleClass().add("text-filter-box-invalid");
			}
		} else {
			getStyleClass().remove("text-filter-box-invalid");
		}
		callUpdateEvent();
	}

	private void onComparator(TextFilter filter) {
		filter.setComparator(comparator.getSelectionModel().getSelectedItem());
		callUpdateEvent();
	}
}
