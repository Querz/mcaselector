package net.querz.mcaselector.ui.component.filter;

import javafx.scene.layout.VBox;
import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;

public class GroupFilterBox extends FilterBox {

	// has a listview in the bottom part of the border pane
	protected VBox filters = new VBox();

	public GroupFilterBox(FilterBox parent, GroupFilter filter, boolean root) {
		super(parent, filter, root);
		setFilter(parent, filter, root);
	}

	public void setFilter(GroupFilter filter) {
		setFilter(null, filter, true);
	}

	private void setFilter(FilterBox parent, GroupFilter filter, boolean root) {
		super.setFilter(filter);
		filters.getChildren().clear();
		add.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_ADD_TOOLTIP));
		delete.setVisible(!root);
		move.setVisible(!root);

		if (root) {
			getStyleClass().add("group-filter-box-root");
		} else {
			getStyleClass().add("group-filter-box");
		}

		if (!filter.getFilterValue().isEmpty() || root && parent == null) {
			type.getItems().clear();
			type.getItems().addAll(FilterType.GROUP, FilterType.NOT_GROUP);
			type.getSelectionModel().select(filter.getType());
		}

		for (Filter<?> f : filter.getFilterValue()) {
			if (f instanceof NumberFilter) {
				filters.getChildren().add(new NumberFilterBox(this, (NumberFilter<?>) f, root));
			} else if (f instanceof TextFilter) {
				filters.getChildren().add(new TextFilterBox(this, (TextFilter<?>) f, root));
			} else if (f instanceof GroupFilter) {
				filters.getChildren().add(new GroupFilterBox(this, (GroupFilter) f, false));
			} else {
				throw new RuntimeException("failed to display filter " + f.getClass().getSimpleName());
			}
		}
		setBottom(filters);
	}
}
