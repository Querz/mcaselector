package net.querz.mcaselector.ui;

import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.DataVersionFilter;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.filter.NumberFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.util.Helper;
import java.util.function.Consumer;

public abstract class FilterBox extends BorderPane {

	private static final Image deleteIcon = Helper.getIconFromResources("img/delete");
	private static final Image addIcon = Helper.getIconFromResources("img/add");

	private Filter filter;
	private FilterBox parent;

	protected Label delete = new Label("", new ImageView(deleteIcon));
	protected Label add = new Label("", new ImageView(addIcon));
	protected ComboBox<FilterType> type = new ComboBox<>();
	protected ComboBox<Operator> operator = new ComboBox<>();
	protected GridPane filterOperators = new GridPane();

	private Consumer<FilterBox> updateListener;
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
		controls.add(add, 0, 0, 1, 1);
		controls.add(delete, 1, 0, 1, 1);

		setRight(controls);

		filterOperators.getStyleClass().add("filter-operators-grid");

		operator.getItems().addAll(Operator.values());
		operator.getSelectionModel().select(filter.getOperator());
		operator.setOnAction(e -> onOperator(filter));
		operator.getStyleClass().add("filter-operator-combo-box");
		filterOperators.add(operator, 1, 0, 1, 1);

		if (filter.getParent() == null || ((GroupFilter) filter.getParent()).getFilterValue().get(0) == filter) {
			operator.setVisible(false);
		}

		type.getItems().addAll(FilterType.values());
		type.getSelectionModel().select(filter.getType());
		type.setOnAction(e -> update(type.getSelectionModel().getSelectedItem()));
		type.getStyleClass().add("filter-type-combo-box");

		filterOperators.setAlignment(Pos.TOP_LEFT);
		filterOperators.add(type, 0, 0, 1, 1);

		setLeft(filterOperators);

		add.setOnMouseReleased(e -> onAdd(filter));
		delete.setOnMouseReleased(e -> onDelete(filter));
	}

	public void setOnUpdate(Consumer<FilterBox> listener) {
		updateListener = listener;
	}

	protected void onAdd(Filter filter) {
		NumberFilter f = new DataVersionFilter(Operator.AND, Comparator.EQ, 0);
		int index;
		if (filter.getParent() == null || filter instanceof GroupFilter) {
			//root group
			index = ((GroupFilter) filter).addFilter(f);
		} else {
			index = ((GroupFilter) filter.getParent()).addFilterAfter(f, filter);
		}
		if (this instanceof GroupFilterBox) {
			NumberFilterBox fb = new NumberFilterBox(this, f, false);
			fb.setText("");
			((GroupFilterBox) this).filters.getChildren().add(index, fb);
			type.setDisable(true);
		} else if (parent instanceof GroupFilterBox) {
			NumberFilterBox fb = new NumberFilterBox(this.parent, f, this.root);
			fb.setText("");
			((GroupFilterBox) parent).filters.getChildren().add(index, fb);
		}

		callUpdateEvent();
	}

	protected void onDelete(Filter filter) {
		((GroupFilter) filter.getParent()).removeFilter(filter);
		if (parent instanceof GroupFilterBox) {
			((GroupFilterBox) parent).filters.getChildren().remove(this);
			if (((GroupFilterBox) parent).filters.getChildren().isEmpty()) {
				if (parent.parent != null) {
					((GroupFilterBox) parent).type.setDisable(false);
				}
			} else {
				((FilterBox) ((GroupFilterBox) parent).filters.getChildren().get(0)).operator.setVisible(false);
			}
		}

		callUpdateEvent();
	}

	protected void update(FilterType type) {
		if (type != filter.getType()) {
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
		callUpdateEvent();
	}

	private void onOperator(Filter filter) {
		filter.setOperator(operator.getSelectionModel().getSelectedItem());
		callUpdateEvent();
	}

	protected void callUpdateEvent() {
		FilterBox p = this;
		while (p != null) {
			if (p.updateListener != null) {
				p.updateListener.accept(this);
			}
			p = p.parent;
		}
	}
}
