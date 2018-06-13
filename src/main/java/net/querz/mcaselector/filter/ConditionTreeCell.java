package net.querz.mcaselector.filter;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class ConditionTreeCell extends TreeCell<Filter> {

	//[AND|OR v] [type v] [>|<|= v] [value txt] [+] [x]

	public HBox box = new HBox();

	private ComboBox<Operator> operator = new ComboBox<>();
	private ComboBox<ConditionType> type = new ComboBox<>();
	private ComboBox<Comparator> comparator = new ComboBox<>();
	//TODO: typesafe values, see GotoDialog
	private TextField value = new TextField();
	private Button add = new Button("+");
	private Button delete = new Button("x");

	public ConditionTreeCell() {
		operator.getItems().addAll(Operator.values());
		operator.getSelectionModel().selectFirst();
		type.getItems().addAll(ConditionType.values());
		type.getSelectionModel().selectFirst();
		comparator.getItems().addAll(Comparator.values());
		comparator.getSelectionModel().selectFirst();

		box.getChildren().addAll(
				operator,
				type,
				comparator,
				value,
				add,
				delete
		);
		box.getStyleClass().add("filter-cell-box");
		setGraphic(box);
	}

	@Override
	public void startEdit() {
		System.out.println("start");
		if (!isEditable() || !getTreeView().isEditable()) {
			return;
		}
		super.startEdit();
		setGraphic(box);
	}

	@Override
	public void commitEdit(Filter filter) {
		System.out.println("commit");
		super.commitEdit(filter);
	}

	@Override
	public void cancelEdit() {
		System.out.println("cancel");
		super.cancelEdit();
		setGraphic(getTreeItem().getGraphic());
	}

	@Override
	protected void updateItem(Filter filter, boolean empty) {
		System.out.println("update");
		super.updateItem(filter, empty);

		if (!empty && filter != null) {
			operator.getSelectionModel().select(filter.getOperator());
			type.getSelectionModel().select(ConditionType.match(filter));
			comparator.getSelectionModel().select(filter.getComparator());
			setGraphic(box);
		} else {
			setGraphic(null);
		}
	}
}
