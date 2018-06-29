package net.querz.mcaselector.filter;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import net.querz.mcaselector.filter.structure.Comparator;
import net.querz.mcaselector.filter.structure.DataVersionFilter;
import net.querz.mcaselector.filter.structure.Filter;
import net.querz.mcaselector.filter.structure.FilterType;
import net.querz.mcaselector.filter.structure.GroupFilter;
import net.querz.mcaselector.filter.structure.NumberFilter;
import net.querz.mcaselector.filter.structure.Operator;
import net.querz.mcaselector.util.Helper;

public abstract class FilterBox extends BorderPane {

	private static final Image deleteIcon = Helper.getIconFromResources("img/delete");
	private static final Image addIcon = Helper.getIconFromResources("img/add");
	private static final Image moveImage = Helper.getIconFromResources("img/move");

	private Filter filter;
	Label delete = new Label("", new ImageView(deleteIcon));
	Label add = new Label("", new ImageView(addIcon));
	Label move = new Label("", new ImageView(moveImage));

	FilterBox parent;

	ComboBox<FilterType> type = new ComboBox<>();
	ComboBox<Operator> operator = new ComboBox<>();

	GridPane filterOperators = new GridPane();

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
//		controls.add(move, 0, 0, 1, 1);
		controls.add(add, 1, 0, 1, 1);
		controls.add(delete, 2, 0, 1, 1);

		if (this instanceof GroupFilterBox && root) {
			delete.setVisible(false);
			move.setVisible(false);
		}

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

		//TODO: move
//		move.setOnDragDetected(this::onDragDetected);
//		setOnDragOver(this::onDragOver);
//		setOnDragDropped(this::onDragDropped);
	}

	protected void onAdd(Filter filter) {
		NumberFilter f = new DataVersionFilter(Operator.AND, Comparator.EQ, 1344);

		int index;

		if (filter.getParent() == null || filter instanceof GroupFilter) {
			//root group
			index = ((GroupFilter) filter).addFilter(f);
		} else {
			index = ((GroupFilter) filter.getParent()).addFilterAfter(f, filter);
		}

		Filter root = getRoot(filter);

		if (this instanceof GroupFilterBox) {
			((GroupFilterBox) this).filters.getChildren().add(index, new NumberFilterBox(this, f, false));
			type.setDisable(true);
		} else if (parent instanceof GroupFilterBox) {
			((GroupFilterBox) parent).filters.getChildren().add(index, new NumberFilterBox(this.parent, f, this.root));
		}
	}

	protected void onDelete(Filter filter) {
		Filter root = getRoot(filter);
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
	}

	private void onOperator(Filter filter) {
		filter.setOperator(operator.getSelectionModel().getSelectedItem());
	}

	private static final DataFormat dragDataFormat = new DataFormat("filter.content");

	private void onDragDetected(MouseEvent event) {
		System.out.println("onDragDetected " + event.getX() + " " + event.getY());
		Bounds m = move.localToScene(move.getBoundsInLocal());
		Bounds p = localToScene(getBoundsInLocal());
		double x = m.getMinX() - p.getMinX();
		double y = m.getMinY() - p.getMinY();
		System.out.println(x + " " + y);

		Dragboard db = startDragAndDrop(TransferMode.MOVE);
		WritableImage image = snapshot(null, null);
//		try {
//			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new File("src/main/resources/test.png"));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		db.setDragView(image);
		System.out.println(image.getWidth());

		ClipboardContent content = new ClipboardContent();
		content.put(dragDataFormat, filter);
		db.setContent(content);

		event.consume();
	}

	private void onDragOver(DragEvent e) {
		if (e.getDragboard().hasContent(dragDataFormat)) {
			e.acceptTransferModes(TransferMode.MOVE);
		}
		e.consume();
	}

	private void onDragDropped(DragEvent e) {
		if (e.getDragboard().hasContent(dragDataFormat)) {
			Filter source = (Filter) e.getDragboard().getContent(dragDataFormat);
		}
		e.consume();
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
