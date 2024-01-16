package net.querz.mcaselector.ui.component.filter;

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
import net.querz.mcaselector.filter.filters.DataVersionFilter;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.filter.NumberFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.mcaselector.ui.component.FileTextField;

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
	private final boolean root;

	private static Filter<?> dragDropFilter;
	private static FilterBox dragDropFilterBox;

	private static GroupFilterBox lastFocusedGroupFilterBox;

	private static FilterBox currentDragDropTarget;
	private static int currentDragDropTargetDirection;

	private static final boolean USE_DRAGVIEW_OFFSET;

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		USE_DRAGVIEW_OFFSET = osName.contains("windows");
	}

	private static final String stylesheet = FilterBox.class.getClassLoader().getResource("style/component/filter-box.css").toExternalForm();

	public FilterBox(FilterBox parent, Filter<?> filter, boolean root) {
		this.parent = parent;
		this.root = root;
		if (root) {
			getStyleClass().add("filter-box-root");
		}
		getStyleClass().add("filter-box");

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
		setOnDragEntered(this::onDragEntered);

		setRight(controls);

		filterOperators.getStyleClass().add("filter-operators-grid");
		
		type.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_TYPE_TOOLTIP));
		type.getItems().addAll(FilterType.queuables());
		type.getSelectionModel().select(filter.getType());
		type.setOnAction(e -> update(type.getSelectionModel().getSelectedItem()));
		type.getStyleClass().add("filter-type-combo-box");
		filterOperators.add(type, 1, 0, 1, 1);

		operator.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_FILTER_OPERATOR_TOOLTIP));
		operator.getItems().addAll(Operator.values());
		operator.setOnAction(e -> onOperator(this.filter));
		operator.getStyleClass().add("filter-operator-combo-box");
		filterOperators.add(operator, 0, 0, 1, 1);

		filterOperators.setAlignment(Pos.TOP_LEFT);

		setLeft(filterOperators);

		add.setOnMouseReleased(e -> onAdd(this.filter));
		delete.setOnMouseReleased(e -> onDelete(this.filter));

		setFilter(filter);

		getStylesheets().add(stylesheet);
	}

	private void onDragDetected(MouseEvent e) {
		if (e.getTarget() == move) {
			Dragboard db = startDragAndDrop(TransferMode.MOVE);
			WritableImage wi = new WritableImage((int) getWidth(), (int) getHeight());
			Image dbImg = snapshot(null, wi);
			db.setDragView(dbImg);
			if (USE_DRAGVIEW_OFFSET) {
				db.setDragViewOffsetX(e.getX());
				db.setDragViewOffsetY(e.getY());
			}
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
			// test if this is not a child of dragDropFilterBox
			if (!isChildOf(dragDropFilterBox) && this != dragDropFilterBox && filter.getParent() != null) {
				e.acceptTransferModes(TransferMode.MOVE);

				currentDragDropTarget = this;

				if (this instanceof GroupFilterBox) {
					lastFocusedGroupFilterBox = (GroupFilterBox) this;
					lastFocusedGroupFilterBox.setInsertCssClass("filter-drop-target", null);
					double dropCheckHeight = ((GroupFilterBox) this).filters.getChildren().size() == 0 ? 12 : 18;
					if (e.getY() < dropCheckHeight) {
						currentDragDropTargetDirection = -1;
						setInsertCssClass("filter-drop-target", "before");
					} else if (e.getY() >= getHeight() - dropCheckHeight) {
						currentDragDropTargetDirection = 1;
						setInsertCssClass("filter-drop-target", "after");
					} else {
						setInsertCssClass("filter-drop-target", "into");
						currentDragDropTargetDirection = 0;
					}
				} else if (e.getY() < getHeight() / 2) {
					setInsertCssClass("filter-drop-target", "before");
					currentDragDropTargetDirection = -1;
				} else {
					setInsertCssClass("filter-drop-target", "after");
					currentDragDropTargetDirection = 1;
				}
			}
		}
		e.consume();
	}

	private void onDragEntered(DragEvent e) {
		if (lastFocusedGroupFilterBox != null && this != lastFocusedGroupFilterBox) {
			lastFocusedGroupFilterBox.setInsertCssClass("filter-drop-target", null);
			lastFocusedGroupFilterBox = null;
		}
	}

	private void onDragExited(DragEvent e) {
		setInsertCssClass("filter-drop-target", null);
		currentDragDropTarget = null;
	}

	private void onDragDone(DragEvent e) {
		Dragboard db = e.getDragboard();
		if (db.hasContent(filterBoxDataFormat)) {
			dragDropFilter = null;
			dragDropFilterBox = null;
			currentDragDropTarget = null;
			currentDragDropTargetDirection = 0;
		}
		e.consume();
	}

	private void onDragDropped(DragEvent e) {
		Dragboard db = e.getDragboard();
		if (db.hasContent(filterBoxDataFormat)) {

			// if we have an invalid target
			if (currentDragDropTarget == null) {
				e.consume();
				return;
			}

			// ignore if it is dropped on itself
			if (dragDropFilter != filter) {
				// remove from old parent handle
				GroupFilter oldParent = (GroupFilter) dragDropFilter.getParent();
				oldParent.removeFilter(dragDropFilter);

				// remove from old parent box
				GroupFilterBox oldParentBox = (GroupFilterBox) dragDropFilterBox.parent;
				oldParentBox.filters.getChildren().remove(dragDropFilterBox);

				// fix parents
				if (currentDragDropTargetDirection == 0) { // into
					dragDropFilterBox.parent = currentDragDropTarget;
					dragDropFilter.setParent(currentDragDropTarget.filter);
				} else { // after or before
					dragDropFilterBox.parent = parent;
					dragDropFilter.setParent(filter.getParent());
				}

				// make operator visible
				dragDropFilterBox.operator.setVisible(true);
				currentDragDropTarget.operator.setVisible(true);

				// add to parents
				if (currentDragDropTargetDirection == 0) { // into
					((GroupFilterBox) currentDragDropTarget).filters.getChildren().add(dragDropFilterBox);
					((GroupFilter) currentDragDropTarget.filter).addFilter(dragDropFilter);

					// make first operator in target group invisible
					((FilterBox) ((GroupFilterBox) currentDragDropTarget).filters.getChildren().get(0)).operator.setVisible(false);

				} else {
					int targetIndex = ((GroupFilterBox) currentDragDropTarget.parent).filters.getChildren().indexOf(currentDragDropTarget);
					if (currentDragDropTargetDirection == 1) {
						targetIndex++;
					}

					// add at index in view
					((GroupFilterBox) currentDragDropTarget.parent).filters.getChildren().add(targetIndex, dragDropFilterBox);

					// add at index in handle
					((GroupFilter) currentDragDropTarget.filter.getParent()).getFilterValue().add(targetIndex, dragDropFilter);

					// make first operator in target group invisible
					((FilterBox) ((GroupFilterBox) currentDragDropTarget.parent).filters.getChildren().get(0)).operator.setVisible(false);
				}

				// make first operator on source group invisible
				if (oldParentBox.filters.getChildren().size() > 0) {
					((FilterBox) oldParentBox.filters.getChildren().get(0)).operator.setVisible(false);
				}

				// update external stuff
				callUpdateEvent();
			}
		}
		e.consume();
	}

	private boolean isChildOf(Node node) {
		Node parent = getParent();
		while (parent != null) {
			parent = parent.getParent();
			if (parent == node) {
				return true;
			}
		}
		return false;
	}

	protected void setInsertCssClass(String prefix, String name) {
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
			// root group
			index = ((GroupFilter) filter).addFilter(f);
		} else {
			index = ((GroupFilter) filter.getParent()).addFilterAfter(f, filter);
		}
		if (this instanceof GroupFilterBox) {
			((GroupFilterBox) this).filters.getChildren().add(index, new NumberFilterBox(this, f, false));
			type.getItems().clear();
			type.getItems().addAll(FilterType.GROUP, FilterType.NOT_GROUP);
			type.getSelectionModel().select(filter.getType());
		} else if (parent instanceof GroupFilterBox) {
			((GroupFilterBox) parent).filters.getChildren().add(index, new NumberFilterBox(this.parent, f, this.root));
		}

		callUpdateEvent();
	}

	protected void onDelete(Filter<?> filter) {
		((GroupFilter) filter.getParent()).removeFilter(filter);
		if (parent instanceof GroupFilterBox) {
			((GroupFilterBox) parent).filters.getChildren().remove(this);
			if (!((GroupFilterBox) parent).filters.getChildren().isEmpty()) {
				if (parent.parent != null) {
					parent.type.getItems().clear();
					parent.type.getItems().addAll(FilterType.values());
					parent.type.getSelectionModel().select(parent.filter.getType());
				}
				((FilterBox) ((GroupFilterBox) parent).filters.getChildren().get(0)).operator.setVisible(false);
			}
		}

		callUpdateEvent();
	}

	protected void update(FilterType type) {
		if (type == null) {
			return;
		}

		if (type != filter.getType()) {
			GroupFilter parent = ((GroupFilter) filter.getParent());
			Filter<?> f = null;

			// test if we only switched the group type
			if (filter.getType().getFormat() == FilterType.Format.GROUP && type.getFormat() == FilterType.Format.GROUP) {
				((GroupFilter) filter).setNegated(type == FilterType.NOT_GROUP);
				f = filter;
			} else if (parent != null) {
				// add new filter
				f = type.create();
				parent.addFilterAfter(Objects.requireNonNull(f), filter);
				parent.getFilterValue().remove(filter);

				// use the same value and comparator if the filter format is equal, but not if it's a custom text format
				if (filter.getType().getFormat() == type.getFormat() && filter.getType().getFormat() != FilterType.Format.TEXT) {
					f.setFilterValue(filter.getRawValue());
					f.setComparator(filter.getComparator());
				}
				// always keep the operator
				f.setOperator(filter.getOperator());
			}

			// remove this filter from view and add new filterbox
			FilterBox newBox;
			switch (type.getFormat()) {
				case GROUP -> newBox = new GroupFilterBox(this.parent, (GroupFilter) f, false);
				case TEXT -> newBox = new TextFilterBox(this.parent, (TextFilter<?>) f, false);
				case NUMBER -> newBox = new NumberFilterBox(this.parent, (NumberFilter<?>) f, false);
				case FILE -> newBox = new FileFilterBox(this.parent, (TextFilter<?>) f, false);
				default -> throw new RuntimeException("unknown FilterType format: " + type.getFormat());
			}

			if (this.parent != null) {
				int index = ((GroupFilterBox) this.parent).filters.getChildren().indexOf(this);
				((GroupFilterBox) this.parent).filters.getChildren().remove(index);
				((GroupFilterBox) this.parent).filters.getChildren().add(index, newBox);
			}
		}
		callUpdateEvent();
	}

	protected void setFilter(Filter<?> filter) {
		this.filter = filter;
		operator.getSelectionModel().select(filter.getOperator());

		if (filter.getParent() == null || ((GroupFilter) filter.getParent()).getFilterValue().get(0) == filter) {
			operator.setVisible(false);
		}
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
