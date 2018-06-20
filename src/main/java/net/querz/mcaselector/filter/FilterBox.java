package net.querz.mcaselector.filter;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import net.querz.mcaselector.filter.structure.Comparator;
import net.querz.mcaselector.filter.structure.DataVersionFilter;
import net.querz.mcaselector.filter.structure.Filter;
import net.querz.mcaselector.filter.structure.FilterType;
import net.querz.mcaselector.filter.structure.GroupFilter;
import net.querz.mcaselector.filter.structure.NumberFilter;
import net.querz.mcaselector.filter.structure.Operator;

public abstract class FilterBox extends BorderPane {

	private Filter filter;
	Label delete = new Label(Character.toString((char) 128465));
	Label add = new Label("+");
	Label move = new Label("\u2195");

	private FilterBox parent;

	ComboBox<FilterType> typeComboBox = new ComboBox<>();

	private boolean root;

	public FilterBox(FilterBox parent, Filter filter, boolean root) {
		this.parent = parent;
		this.root = root;
		if (root) {
			getStyleClass().add("filter-box-root");
		}
		getStyleClass().add("filter-box");
		this.filter = filter;

		GridPane controls = new GridPane();
		controls.setAlignment(Pos.TOP_RIGHT);
		controls.add(move, 0, 0, 1, 1);
		controls.add(add, 1, 0, 1, 1);
		if (!(this instanceof GroupFilterBox) || !root) {
			controls.add(delete, 2, 0, 1, 1);
		}

		setRight(controls);

		typeComboBox.getItems().addAll(FilterType.values());
		typeComboBox.getSelectionModel().select(filter.getType());

		GridPane type = new GridPane();
		type.setAlignment(Pos.TOP_LEFT);
		type.add(typeComboBox, 0, 0, 1, 1);

		setLeft(type);

		//TODO: move
		add.setOnMouseReleased(e -> onAdd(filter));
		delete.setOnMouseReleased(e -> onDelete(filter));
		typeComboBox.setOnAction(e -> update(typeComboBox.getSelectionModel().getSelectedItem()));
	}

	protected void onAdd(Filter filter) {
		System.out.println("onAdd");
		NumberFilter f = new DataVersionFilter(Operator.AND, Comparator.EQ, 1344);

		int index;

		if (filter.getParent() == null || filter instanceof GroupFilter) {
			//root group
			index = ((GroupFilter) filter).addFilter(f);
		} else {
			index = ((GroupFilter) filter.getParent()).addFilterAfter(f, filter);
		}


		Filter root = getRoot(filter);
		System.out.println("before: " + root);

		if (this instanceof GroupFilterBox) {
			((GroupFilterBox) this).filters.getChildren().add(index, new NumberFilterBox(this, f, this.root));
			typeComboBox.setDisable(true);
		} else if (parent instanceof GroupFilterBox) {
			((GroupFilterBox) parent).filters.getChildren().add(index, new NumberFilterBox(this.parent, f, this.root));
		}

		System.out.println("after: " + root);
	}

	protected void onDelete(Filter filter) {
		System.out.println("onDelete");

		Filter root = getRoot(filter);
		System.out.println("before: " + root);

		((GroupFilter) filter.getParent()).removeFilter(filter);

		System.out.println("after: " + root);


		if (parent instanceof GroupFilterBox) {
			((GroupFilterBox) parent).filters.getChildren().remove(this);
			if (((GroupFilterBox) parent).filters.getChildren().isEmpty()) {
				((GroupFilterBox) parent).typeComboBox.setDisable(false);
			}
		}
	}

	protected void update(FilterType type) {
		if (type != filter.getType()) {
			System.out.println("update");
			Filter newFilter = type.create();

			//removes this filter and replaces it by a new filter
			GroupFilter parent = ((GroupFilter) filter.getParent());
			parent.addFilterAfter(newFilter, filter);
			parent.getFilterValue().remove(filter);

			//remove this filter from view and add new filterbox

			FilterBox newBox;

			if (type == FilterType.GROUP) {
				newBox = new GroupFilterBox(this.parent, (GroupFilter) newFilter, root);
			} else {
				newBox = new NumberFilterBox(this.parent, (NumberFilter) newFilter, root);
			}

			int index = ((GroupFilterBox) this.parent).filters.getChildren().indexOf(this);
			((GroupFilterBox) this.parent).filters.getChildren().remove(index);
			((GroupFilterBox) this.parent).filters.getChildren().add(index, newBox);
		}
	}

	private void onMove(Filter filter) {
		System.out.println("onMove");
	}

	private Filter getRoot(Filter filter) {
		Filter root = filter;
		for (;;) {
			if (root.getParent() == null) {
				return root;
			}
			root = root.getParent();
		}
	}
}
