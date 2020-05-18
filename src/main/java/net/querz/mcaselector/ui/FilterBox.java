package net.querz.mcaselector.ui;

import javafx.event.EventTarget;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.DataVersionFilter;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.filter.NumberFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.text.Translation;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class FilterBox extends BorderPane {

	private static final Image deleteIcon = FileHelper.getIconFromResources("img/delete");
	private static final Image addIcon = FileHelper.getIconFromResources("img/add");
	private static final Image burgerIcon = FileHelper.getIconFromResources("img/burger");

	private static final DataFormat filterBoxDataFormat = new DataFormat("filterbox-dataformat");

	private Filter<?> filter;
	private FilterBox parent;

	protected Label delete = new Label("", new ImageView(deleteIcon));
	protected Label add = new Label("", new ImageView(addIcon));
	protected Label move = new Label("", new ImageView(burgerIcon));
	protected ComboBox<FilterType> type = new ComboBox<>();
	protected ComboBox<Operator> operator = new ComboBox<>();
	protected GridPane filterOperators = new GridPane();

	private Consumer<FilterBox> updateListener;
	private boolean root;

	private static Filter<?> dragDropFilter;
	private static FilterBox dragDropFilterBox;

	public FilterBox(FilterBox parent, Filter<?> filter, boolean root) {
		this.parent = parent;
		this.root = root;
		if (root) {
			getStyleClass().add("filter-box-root");
		}
		getStyleClass().add("filter-box");
		this.filter = filter;

		add.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_ADD_TOOLTIP));
		add.getStyleClass().add("control-label");

		delete.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_DELETE_TOOLTIP));
		delete.getStyleClass().add("control-label");

		GridPane controls = new GridPane();
		controls.getStyleClass().add("filter-controls-grid");
		controls.setAlignment(Pos.TOP_RIGHT);
		controls.add(add, 0, 0, 1, 1);
		controls.add(delete, 1, 0, 1, 1);
		controls.add(move, 2, 0, 1, 1);

		setOnDragDetected(this::onDragDetected);
		setOnDragOver(this::onDragOver);
		setOnDragDone(this::onDragDone);
		setOnDragDropped(this::onDragDropped);
		setOnDragExited(this::onDragExited);

		setRight(controls);

		filterOperators.getStyleClass().add("filter-operators-grid");
		
		type.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_TYPE_TOOLTIP));
		type.getItems().addAll(FilterType.values());
		type.getSelectionModel().select(filter.getType());
		type.setOnAction(e -> update(type.getSelectionModel().getSelectedItem()));
		type.getStyleClass().add("filter-type-combo-box");
		filterOperators.add(type, 1, 0, 1, 1);

		operator.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_OPERATOR_TOOLTIP));
		operator.getItems().addAll(Operator.values());
		operator.getSelectionModel().select(filter.getOperator());
		operator.setOnAction(e -> onOperator(filter));
		operator.getStyleClass().add("filter-operator-combo-box");
		filterOperators.add(operator, 0, 0, 1, 1);

		if (filter.getParent() == null || ((GroupFilter) filter.getParent()).getFilterValue().get(0) == filter) {
			operator.setVisible(false);
		}

		filterOperators.setAlignment(Pos.TOP_LEFT);

		setLeft(filterOperators);

		add.setOnMouseReleased(e -> onAdd(filter));
		delete.setOnMouseReleased(e -> onDelete(filter));
	}

	private void onDragDetected(MouseEvent e) {
		if (e.getTarget() == move) {
			Dragboard db = startDragAndDrop(TransferMode.MOVE);
			WritableImage wi = new WritableImage((int) getWidth(), (int) getHeight());
			Image dbImg = snapshot(null, wi);
			db.setDragView(dbImg);
			db.setDragViewOffsetX(e.getX());
			db.setDragViewOffsetY(e.getY());
			ClipboardContent content = new ClipboardContent();
			content.put(filterBoxDataFormat, filter);
			db.setContent(content);
			dragDropFilter = filter;
			dragDropFilterBox = this;
			e.consume();
		}
	}

	private void onDragOver(DragEvent e) {
		if (e.getDragboard().hasContent(filterBoxDataFormat)) {
			if (this != dragDropFilterBox) {
				e.acceptTransferModes(TransferMode.MOVE);

				double height = getHeight();

				if (this instanceof GroupFilterBox) {

				} else if (e.getY() < height / 2) {

					System.out.println("before");
					// insert before
					setInsertCssClass("filter-drop-target", "before");
				} else {
					System.out.println("after");
					// insert after
					setInsertCssClass("filter-drop-target", "after");
				}
			}
		}
		e.consume();
	}

	private void onDragExited(DragEvent e) {
		setInsertCssClass("filter-drop-target", null);
	}

	private FilterBox getFilterBoxFromTarget(EventTarget t) {
		if (t instanceof Node) {
			Node n = (Node) t;
			while (!(n instanceof FilterBox) && n != null) {
				n = n.getParent();
			}
			return (FilterBox) n;
		}
		return null;
	}

	private void onDragDone(DragEvent e) {
		Dragboard db = e.getDragboard();
		if (db.hasContent(filterBoxDataFormat)) {
			dragDropFilter = null;
			dragDropFilterBox = null;
		}
		e.consume();
	}

	private void onDragDropped(DragEvent e) {
		Dragboard db = e.getDragboard();
		if (db.hasContent(filterBoxDataFormat)) {
			// ignore if it is dropped on itself
			if (dragDropFilter != filter) {
				// remove from old parent handle
				GroupFilter oldParent = (GroupFilter) dragDropFilter.getParent();
				int oldIndex = oldParent.getFilterValue().indexOf(dragDropFilter);
				oldParent.getFilterValue().remove(oldIndex);

				// remove from old parent box
				GroupFilterBox oldParentBox = (GroupFilterBox) dragDropFilterBox.parent;
				oldParentBox.filters.getChildren().remove(dragDropFilterBox);

				// fix parents
				dragDropFilterBox.parent = parent;
				dragDropFilter.setParent(filter.getParent());

				// add to new parent handle
				GroupFilter newParent = (GroupFilter) parent.filter;
				int currentIndex = newParent.getFilterValue().indexOf(filter);

				newParent.getFilterValue().add(currentIndex + 1, dragDropFilter);

				// add to new parent box
				GroupFilterBox newParentBox = (GroupFilterBox) parent;
				newParentBox.filters.getChildren().add(currentIndex + 1, dragDropFilterBox);

			}
		}
		e.consume();
	}

	private void setInsertCssClass(String prefix, String name) {
		String c = prefix + "-" + name;
		for (int i = 0; i < getStyleClass().size(); i++) {
			if (getStyleClass().get(i).startsWith(prefix)) {
				if (name == null) {
					getStyleClass().remove(i);
				} else if (!getStyleClass().get(i).equals(c)) {
					getStyleClass().set(i, c);
				}
				return;
			}
		}
		getStyleClass().add(c);
	}

	public void setOnUpdate(Consumer<FilterBox> listener) {
		updateListener = listener;
	}

	protected void onAdd(Filter<?> filter) {
		NumberFilter<?> f = new DataVersionFilter(Operator.AND, Comparator.EQUAL, 0);
		int index;
		if (filter.getParent() == null || filter instanceof GroupFilter) {
			//root group
			index = ((GroupFilter) filter).addFilter(f);
		} else {
			index = ((GroupFilter) filter.getParent()).addFilterAfter(f, filter);
		}
		if (this instanceof GroupFilterBox) {
			((GroupFilterBox) this).filters.getChildren().add(index, new NumberFilterBox(this, f, false));
			type.setDisable(true);
		} else if (parent instanceof GroupFilterBox) {
			((GroupFilterBox) parent).filters.getChildren().add(index, new NumberFilterBox(this.parent, f, this.root));
		}

		callUpdateEvent();
	}

	protected void onDelete(Filter<?> filter) {
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
			Filter<?> newFilter = type.create();

			//removes this filter and replaces it by a new filter
			GroupFilter parent = ((GroupFilter) filter.getParent());
			parent.addFilterAfter(Objects.requireNonNull(newFilter), filter);
			parent.getFilterValue().remove(filter);

			//remove this filter from view and add new filterbox
			FilterBox newBox = null;
			switch (type.getFormat()) {
			case GROUP:
				newBox = new GroupFilterBox(this.parent, (GroupFilter) newFilter, false);
				break;
			case TEXT:
				newBox = new TextFilterBox(this.parent, (TextFilter<?>) newFilter, false);
				break;
			case NUMBER:
				newBox = new NumberFilterBox(this.parent, (NumberFilter<?>) newFilter, false);
				break;
			default:
				Debug.dump("unknown FilterType Format: " + type.getFormat());
			}

			int index = ((GroupFilterBox) this.parent).filters.getChildren().indexOf(this);
			((GroupFilterBox) this.parent).filters.getChildren().remove(index);
			((GroupFilterBox) this.parent).filters.getChildren().add(index, newBox);
		}
		callUpdateEvent();
	}

	private void onOperator(Filter<?> filter) {
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
