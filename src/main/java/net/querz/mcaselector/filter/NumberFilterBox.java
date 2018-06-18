package net.querz.mcaselector.filter;

import javafx.scene.control.TextField;

public class NumberFilterBox extends FilterBox {

	private TextField input = new TextField();

	public NumberFilterBox(NumberFilter filter) {
		super(filter);
		input.setPromptText(filter.getType().toString());
		input.setText(filter.getFilterValue().toString());
		input.textProperty().addListener((a, b, c) -> onTextInput(filter, b, c));

		setCenter(input);
	}

	private void onTextInput(Filter filter, String oldValue, String newValue) {
		if (!filter.setFilterValue(newValue)) {
			input.setText(oldValue);
		}
	}
}
