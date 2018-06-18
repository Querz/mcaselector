package net.querz.mcaselector.filter;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public class FilterBox extends BorderPane {

	private Filter filter;
	Label delete = new Label(Character.toString((char) 128465));
	Label add = new Label("+");
	Label move = new Label("\u2195");

	ComboBox<FilterType> typeComboBox = new ComboBox<>();

	public FilterBox(Filter filter) {
		this.filter = filter;

		GridPane controls = new GridPane();
		controls.setAlignment(Pos.CENTER_RIGHT);
		controls.add(move, 0, 0, 1, 1);
		controls.add(add, 1, 0, 1, 1);
		controls.add(delete, 2, 0, 1, 1);

		setRight(controls);

		typeComboBox.getItems().addAll(FilterType.values());

		//TODO: move
		add.setOnMouseReleased(e -> onAdd(filter));
		delete.setOnMouseReleased(e -> onDelete(filter));
	}

	private void onAdd(Filter filter) {
		System.out.println("onAdd");
		((GroupFilter) filter.getParent()).addFilterAfter(
				new DataVersionFilter(Operator.AND, Comparator.EQ, 1344), filter);
	}

	private void onDelete(Filter filter) {
		System.out.println("onDelete");
		((GroupFilter) filter.getParent()).removeFilter(filter);
	}

	private void onMove(Filter filter) {
		System.out.println("onMove");
	}
}
