package net.querz.mcaselector.ui;

import javafx.scene.layout.VBox;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.filter.NumberFilter;
import net.querz.mcaselector.util.Debug;

public class GroupFilterBox extends FilterBox {

	//has a listview in the bottom part of the border pane
	protected VBox filters = new VBox();

	public GroupFilterBox(FilterBox parent, GroupFilter filter, boolean root) {
		super(parent, filter, root);
		delete.setVisible(!root);

		if (root) {
			getStyleClass().add("group-filter-box-root");
		} else {
			getStyleClass().add("group-filter-box");
		}

		if (!filter.getFilterValue().isEmpty() || root && parent == null) {
			type.setDisable(true);
		} else {
			type.setDisable(false);
		}

		for (Filter f : filter.getFilterValue()) {
			if (f instanceof NumberFilter) {
				filters.getChildren().add(new NumberFilterBox(this, (NumberFilter) f, root));
			} else if (f instanceof GroupFilter) {
				filters.getChildren().add(new GroupFilterBox(this, (GroupFilter) f, false));
			} else {
				Debug.error("cannot display filter " + f.getClass().getSimpleName());
			}
		}
		setBottom(filters);
	}
}
