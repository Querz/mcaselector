package net.querz.mcaselector.ui.component;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import net.querz.nbt.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import static net.querz.nbt.Tag.Type.*;

public class NBTTreeView extends TreeView<NBTTreeView.NamedTag> {

	private ScrollBar verticalScrollBar = null;
	private NBTTreeItem dragItem; // reference keeping track of which item is being dragged
	private Rectangle dropHighlight = null;
	private static final String dropHighlightCssClass = "drop-highlight";
	private NBTTreeItem dropTarget = null;
	private int dropTargetIndex = 0;
	private Tag copyItem;
	private String copyName;
	private Function<Object, Optional<EditArrayResult>> arrayEditor = null;

	public NBTTreeView() {
		getStyleClass().add("nbt-tree-view");
		getStylesheets().add(Objects.requireNonNull(NBTTreeView.class.getClassLoader().getResource("style/component/nbt-tree-view.css")).toExternalForm());
		setCellFactory(tv -> new NBTTreeCell());
		setEditable(true);
		setOnKeyPressed(this::onKeyPressed);
	}

	public void load(CompoundTag root) {
		super.setRoot(toTreeItem(0, null, root, null));
		getRoot().setExpanded(true);
	}

	public void setArrayEditor(Function<Object, Optional<EditArrayResult>> arrayEditor) {
		this.arrayEditor = arrayEditor;
	}

	public void setOnSelectionChanged(Consumer<Boolean> onSelectionChanged) {
		getSelectionModel().selectedItemProperty().addListener((i, o, n) -> onSelectionChanged.accept(n == null));
	}

	public void deleteSelectedItem() {
		NBTTreeItem selectedItem = (NBTTreeItem) getSelectionModel().getSelectedItem();
		if (selectedItem.getParent() == null) {
			setRoot(null);
		} else {
			NBTTreeItem oldParent = (NBTTreeItem) selectedItem.getParent();
			if (selectedItem.getValue().parent.getType() == COMPOUND) {
				((CompoundTag) selectedItem.getValue().parent).remove(selectedItem.getValue().name);
				oldParent.getChildren().remove(selectedItem);
			} else if (selectedItem.getValue().parent.getType() == LIST) {
				((ListTag) selectedItem.getValue().parent).remove(selectedItem.getValue().index);
				oldParent.getChildren().remove(selectedItem.getValue().index);
				oldParent.updateIndexes();
			}
		}
	}

	public boolean addItemAtSelected(String name, Tag tag, boolean forceEdit) {
		if (tag.getType() == END) {
			return false;
		}

		// if there is no root, we only allow adding a compound tag
		if (getRoot() == null) {
			if (tag.getType() == COMPOUND) {
				load((CompoundTag) tag);
				layout();
				getSelectionModel().select(0);
				return true;
			}
			return false;
		}

		NBTTreeItem target = (NBTTreeItem) getSelectionModel().getSelectedItem();
		NBTTreeItem newItem = null;
		// if this is a list, we add the tag at the last index
		if (target.getValue().ref.getType() == LIST) {
			ListTag list = (ListTag) target.getValue().ref;
			if (list.getElementType() == null || list.getElementType() == tag.getType()) {
				list.addLast(tag);
				target.setExpanded(true);
				target.getChildren().addLast(newItem = toTreeItem(list.size() - 1, null, tag, list));
			}
		} else if (target.getValue().ref.getType() == COMPOUND) {
			newItem = toTreeItem(0, null, tag, target.getValue().ref);
			newItem.getValue().nextPossibleName((CompoundTag) target.getValue().ref, name);
			((CompoundTag) target.getValue().ref).put(newItem.getValue().name, tag);
			target.setExpanded(true);
			target.getChildren().add(newItem);

			// if the parent is a list, we add the tag after the selected tag
		} else if (target.getValue().parent.getType() == LIST) {
			ListTag list = (ListTag) target.getValue().parent;
			if (list.getElementType() == null || list.getElementType() == tag.getType()) {
				int index = target.getValue().index + 1;
				list.add(index, tag);
				target.getParent().getChildren().add(index, newItem = toTreeItem(index, null, tag, list));
				((NBTTreeItem) target.getParent()).updateIndexes();
			}
		} else if (target.getValue().parent.getType() == COMPOUND) {
			newItem = toTreeItem(0, null, tag, target.getValue().parent);
			newItem.getValue().nextPossibleName((CompoundTag) target.getValue().parent, name);
			((CompoundTag) target.getValue().parent).put(newItem.getValue().name, tag);
			target.getParent().getChildren().add(newItem);
		}

		if (newItem == null) {
			return false;
		}

		requestFocus();
		layout(); // refresh layout so we can select and scroll to the new item
		getSelectionModel().select(newItem);
		// only scroll if the item is not visible on screen
		if (!((NBTTreeItem) getSelectionModel().getSelectedItem()).isVisibleOnScreen()) {
			scrollTo(getSelectionModel().getSelectedIndex());
		}
		layout(); // refresh layout again because of a bug in javafx where switching to edit mode might sometimes not work (?)

		if (    // adding an item to a selected list: only start edit on non-containers
				target.getValue().ref.getType() == LIST && !newItem.getValue().isContainerType()
						// adding an item to a selected compound: start edit in all cases
						|| target.getValue().ref.getType() == COMPOUND
						// adding an item to a parent compound: start edit in all cases
						|| !target.getValue().isContainerType() && target.getValue().parent != null && (target.getValue().parent.getType() == COMPOUND
						// adding an item to a parent list: only start edit on non-containers
						|| target.getValue().parent.getType() == LIST && !newItem.getValue().isContainerType())) {

			// only edit if we force it or if the name changed
			if (forceEdit || newItem.getValue().name != null && !name.equals(newItem.getValue().name)) {
				edit(newItem);
			}
		}
		return true;
	}

	public Tag.Type[] getPossibleChildTagTypesFromSelected() {
		NBTTreeItem target = (NBTTreeItem) getSelectionModel().getSelectedItem();
		if (target == null) {
			if (getRoot() == null) {
				return new Tag.Type[]{COMPOUND};
			}
			return new Tag.Type[0];
		}

		// if this is a non-empty list tag, we return its element type
		if (target.getValue().ref.getType() == LIST) {
			Tag.Type elementType = ((ListTag) target.getValue().ref).getElementType();
			if (elementType != null) {
				return new Tag.Type[]{elementType};
			}
		}

		// if this is a primitive tag, but the parent is a list we return its element type
		if (!target.getValue().isContainerType() && target.getValue().parent.getType() == LIST) {
			Tag.Type elementType = ((ListTag) target.getValue().parent).getElementType();
			if (elementType != null) {
				return new Tag.Type[]{elementType};
			}
		}

		// otherwise we return all possibilities
		return new Tag.Type[]{BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY};
	}

	private NBTTreeItem toTreeItem(int index, String name, Tag ref, Tag parent) {
		NBTTreeItem item;
		switch (ref.getType()) {
		case END:
			throw new IllegalArgumentException("NBTTreeItem does not support END tag");
		case LIST:
			item = new NBTTreeItem(new NamedTag(index, name, ref, parent));
			ListTag list = (ListTag) ref;
			for (int i = 0; i < list.size(); i++) {
				item.getChildren().add(toTreeItem(i, null, list.get(i), ref));
			}
			return item;
		case COMPOUND:
			item = new NBTTreeItem(new NamedTag(index, name, ref, parent));
			for (Map.Entry<String, Tag> child : (CompoundTag) ref) {
				item.getChildren().add(toTreeItem(0, child.getKey(), child.getValue(), ref));
			}
			return item;
		default:
			return new NBTTreeItem(new NamedTag(index, name, ref, parent));
		}
	}

	private void onKeyPressed(KeyEvent e) {
		switch (e.getCode()) {
		case C:
			if (e.isShortcutDown()) {
				copySelectedItem();
			}
			break;
		case V:
			if (!e.isShortcutDown()) {
				break;
			}
		case INSERT:
			if (copyItem != null) {
				addItemAtSelected(copyName == null ? "Unknown" : copyName, copyItem, false);
			}
			break;
		case X:
			if (e.isShortcutDown()) {
				copySelectedItem();
			}
		case DELETE:
			deleteSelectedItem();
			break;
		}
	}

	private void copySelectedItem() {
		NBTTreeItem item = (NBTTreeItem) getSelectionModel().getSelectedItem();
		if (item != null) {
			copyItem = item.getValue().ref.copy();
			copyName = item.getValue().name;
		}
	}

	public static class NamedTag implements Serializable {

		private int index;
		private String name;
		private Tag ref;
		private Tag parent;

		public NamedTag(int index, String name, Tag ref, Tag parent) {
			this.index = index;
			this.name = name;
			this.ref = ref;
			this.parent = parent;
		}

		public Tag getRef() {
			return ref;
		}

		public boolean updateValue(String raw) {
			try {
				Consumer<Tag> c = t -> {
					switch (parent.getType()) {
					case COMPOUND -> ((CompoundTag) parent).put(name, t);
					case LIST -> ((ListTag) parent).set(index, t);
					}
					ref = t;
				};
				switch (ref.getType()) {
				case BYTE -> c.accept(ByteTag.valueOf(Byte.parseByte(raw)));
				case SHORT -> c.accept(ShortTag.valueOf(Short.parseShort(raw)));
				case INT -> c.accept(IntTag.valueOf(Integer.parseInt(raw)));
				case LONG -> c.accept(LongTag.valueOf(Long.parseLong(raw)));
				case FLOAT -> c.accept(FloatTag.valueOf(Float.parseFloat(raw)));
				case DOUBLE -> c.accept(DoubleTag.valueOf(Double.parseDouble(raw)));
				case STRING -> c.accept(StringTag.valueOf(raw));
				}
			} catch (Exception e) {
				return false;
			}
			return true;
		}

		public boolean isRoot() {
			return parent == null;
		}

		public boolean isArrayType() {
			return ref.getType() == BYTE_ARRAY || ref.getType() == INT_ARRAY || ref.getType() == LONG_ARRAY;
		}

		public boolean isContainerType() {
			return ref.getType() == COMPOUND || ref.getType() == LIST;
		}

		public boolean isEmptyContainerType() {
			return ref.getType() == COMPOUND && ((CompoundTag) ref).isEmpty() || ref.getType() == LIST && ((ListTag) ref).isEmpty();
		}

		public boolean isNonEmptyContainerType() {
			return ref.getType() == COMPOUND && !((CompoundTag) ref).isEmpty() || ref.getType() == LIST && !((ListTag) ref).isEmpty();
		}

		public boolean isNumericType() {
			return ref.getType().isNumber;
		}

		public String getLabelString() {
			return switch (ref.getType()) {
				case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING ->
						(name == null ? "" : name + ": ") + valueToString();
				case LIST, BYTE_ARRAY, INT_ARRAY, LONG_ARRAY ->
						(name == null ? "(" : name + " (") + ((CollectionTag<?>) ref).size() + ")";
				case COMPOUND ->
						(name == null ? "(" : name + " (") + ((CompoundTag) ref).size() + ")";
				default -> null;
			};
		}

		public String valueToString() {
			return switch (ref.getType()) {
				case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> ((NumberTag) ref).asNumber().toString();
				case STRING -> ((StringTag) ref).getValue();
				default -> null;
			};
		}

		public boolean nextPossibleName(CompoundTag target, String name) {
			if (target.containsKey(name)) {
				int num = 1;
				String numName;
				while (target.containsKey(numName = name + num)) {
					num++;
				}
				this.name = numName;
				return true;
			} else {
				this.name = name;
				return false;
			}
		}

		public Object getArray() {
			return switch (ref.getType()) {
				case BYTE_ARRAY -> ((ByteArrayTag) ref).getValue();
				case INT_ARRAY -> ((IntArrayTag) ref).getValue();
				case LONG_ARRAY -> ((LongArrayTag) ref).getValue();
				default -> throw new IllegalStateException("not an array tag: " + ref.getType());
			};
		}

		public void setArray(Object array) {
			switch (ref.getType()) {
			case BYTE_ARRAY -> ((ByteArrayTag) ref).setValue((byte[]) array);
			case INT_ARRAY -> ((IntArrayTag) ref).setValue((int[]) array);
			case LONG_ARRAY -> ((LongArrayTag) ref).setValue((long[]) array);
			default -> throw new IllegalArgumentException("incompatible array types: " + ref.getType() + " and " + array.getClass());
			}
		}

		@Override
		public String toString() {
			return String.format("index=%d, name=%s, ref=%s, parent=%s",
					index, name, ref.getType(), parent == null ? null : parent.getType());
		}
	}

	class NBTTreeItem extends TreeItem<NamedTag> {

		private static final DataFormat clipboardDataformat = new DataFormat("nbt-tree-item");

		public NBTTreeItem(NamedTag tag) {
			super(tag);
		}

		// override isLeaf to make the disclosure node visible on every item
		@Override
		public boolean isLeaf() {
			return false;
		}

		public boolean isLast() {
			if (getParent() == null) {
				return true;
			}
			ObservableList<TreeItem<NamedTag>> items = getParent().getChildren();
			return this == getParent().getChildren().get(items.size() - 1);
		}

		// get the amount of expanded items and all its expanded child items recursively
		public int countExpandedItems() {
			if (!isExpanded()) {
				return 1;
			}
			int count = 1;
			for (TreeItem<NamedTag> child : getChildren()) {
				count += ((NBTTreeItem) child).countExpandedItems();
			}
			return count;
		}

		// counts all items and the child items of expanded items above this item until reaching parent
		public int getIndexInExpandedHierarchy(NBTTreeItem parent) {
			int count = 0;
			TreeItem<NamedTag> current = this;
			while (current != null && current != parent) {
				for (int i = current.getParent().getChildren().indexOf(current); i >= 0; i--) {
					TreeItem<NamedTag> sibling = current.getParent().getChildren().get(i);
					count += ((NBTTreeItem) sibling).countExpandedItems();
				}
				current = current.getParent();
			}
			return count;
		}

		public void updateIndexes() {
			if (getValue().ref.getType() != LIST) {
				return;
			}
			int i = 0;
			for (TreeItem<NamedTag> child : getChildren()) {
				child.getValue().index = i++;
			}
		}

		public void moveHere(NBTTreeItem source, int dropTargetIndex) {
			NBTTreeItem oldParent = (NBTTreeItem) source.getParent();
			int oldIndex = source.getParent().getChildren().indexOf(source);
			source.getParent().getChildren().remove(oldIndex);
			String oldName = source.getValue().name;

			// when removing an item from a list, we need to update all indexes of its children
			oldParent.updateIndexes();

			// when moving an item down in the order of a list, we need to adjust the target index
			if (oldParent == this && oldIndex < dropTargetIndex) {
				dropTargetIndex--;
			}

			getChildren().add(dropTargetIndex >= 0 ? dropTargetIndex : getChildren().size(), source);

			// set name in NamedTag
			boolean startEdit = false;
			if (getValue().ref.getType() == LIST) {
				source.getValue().name = null;
			} else if (getValue().ref.getType() == COMPOUND && source.getValue().name == null) {
				source.getValue().nextPossibleName((CompoundTag) getValue().ref, "Unknown");
				startEdit = true;
			} else {
				startEdit = source.getValue().nextPossibleName((CompoundTag) getValue().ref, source.getValue().name);
			}

			// remove tag in source nbt data
			if (source.getValue().parent.getType() == LIST) {
				((ListTag) source.getValue().parent).remove(source.getValue().ref);
			} else if (source.getValue().parent.getType() == COMPOUND) {
				((CompoundTag) source.getValue().parent).remove(oldName);
			}

			// set new parent in NamedTag
			source.getValue().parent = getValue().ref;

			// add tag to target nbt
			if (getValue().ref.getType() == LIST) {
				((ListTag) getValue().ref).add(dropTargetIndex, source.getValue().ref);
				updateIndexes();
			} else if (getValue().ref.getType() == COMPOUND) {
				((CompoundTag) getValue().ref).put(source.getValue().name, source.getValue().ref);
				source.getValue().index = 0;
			}

			// if we had to add or change the name, we start edit mode
			if (startEdit) {
				NBTTreeView.this.layout();
				NBTTreeView.this.scrollTo(NBTTreeView.this.getRow(source));
				NBTTreeView.this.getSelectionModel().select(source);
				Platform.runLater(() -> NBTTreeView.this.edit(source));
			}
		}

		public boolean isVisibleOnScreen() {
			Set<Node> treeCells = NBTTreeView.this.lookupAll(".tree-cell");
			for (Node treeCell : treeCells) {
				if (((NBTTreeCell) treeCell).getTreeItem() == this) {
					return true;
				}
			}
			return false;
		}
	}

	public class NBTTreeCell extends TreeCell<NamedTag> {

		private HBox box;
		private TextField name;
		private TextField value;
		private Button edit; // for array types, show edit button instead of value text field that opens array editor

		private static final boolean noDragviewOffset = System.getProperty("os.name").toLowerCase().contains("win");
		private static final String nameFieldCssClass = "name-text-field";
		private static final String valueFieldCssClass = "value-text-field";
		private static final String editButtonCssClass = "edit-button";
		private static final String expandDisclosureNodeCssClass = "expand"; // the lines making up the + / - sign of the disclosure node
		private static final String expandSquareDisclosureNodeCssClass = "expand-square"; // the square around the + / - sign of the disclosure node
		private static final Image[] icons = new Image[13];
		static {
			ClassLoader cl = NBTTreeCell.class.getClassLoader();
			Function<String, Image> f = s -> new Image(Objects.requireNonNull(cl.getResourceAsStream(s)));
			icons[BYTE.id] = f.apply("img/nbt/byte.png");
			icons[SHORT.id] = f.apply("img/nbt/short.png");
			icons[INT.id] = f.apply("img/nbt/int.png");
			icons[LONG.id] = f.apply("img/nbt/long.png");
			icons[FLOAT.id] = f.apply("img/nbt/float.png");
			icons[DOUBLE.id] = f.apply("img/nbt/double.png");
			icons[STRING.id] = f.apply("img/nbt/string.png");
			icons[LIST.id] = f.apply("img/nbt/list.png");
			icons[COMPOUND.id] = f.apply("img/nbt/compound.png");
			icons[BYTE_ARRAY.id] = f.apply("img/nbt/byte_array.png");
			icons[INT_ARRAY.id] = f.apply("img/nbt/int_array.png");
			icons[LONG_ARRAY.id] = f.apply("img/nbt/long_array.png");
		}

		public NBTTreeCell() {
			getStyleClass().add("nbt-tree-cell");
			setOnDragDetected(this::onDragDetected);
			setOnDragOver(this::onDragOver);
			setOnDragEntered(this::onDragOver);
			setOnDragDropped(this::onDragDropped);
			setOnDragExited(this::onDragExited);
			setOnDragDone(this::onDragDone);
		}

		public static Image getIcon(Tag.Type type) {
			return icons[type.id];
		}

		@Override
		public void startEdit() {
			if (!isEditable() || !getTreeView().isEditable()) {
				return;
			}
			super.startEdit();
			if (!isEditing()) {
				return;
			}

			if (box == null) {
				box = new HBox();
				box.setAlignment(Pos.CENTER_LEFT);
			}

			TextField focus = null;

			if (getItem().parent.getType() == COMPOUND) {
				if (getItem().isArrayType()) {
					// array inside compound: [name, edit]
					box.getChildren().setAll(getGraphic(), focus = nameField(), editButton());
				} else if (getItem().isContainerType()) {
					// container inside compound: [name]
					box.getChildren().setAll(getGraphic(), focus = nameField());
				} else {
					// everything else inside compound: [name, value]
					box.getChildren().setAll(getGraphic(), focus = nameField(), valueField());
				}
			} else if (getItem().parent.getType() == LIST) {
				if (getItem().isArrayType()) {
					// array inside list: [edit]
					box.getChildren().setAll(getGraphic(), editButton());
				} else if (!getItem().isContainerType()) {
					// everything that's not a container: [value]
					box.getChildren().setAll(getGraphic(), focus = valueField());
				} else {
					// container inside list: nothing to edit
					return;
				}
			}

			setGraphic(box);
			setText(null);
			if (focus != null) {
				focus.requestFocus();
				focus.selectAll();
			}
		}

		// overriding cancelEdit to recreate the previous state of the cell allows us to cancel with ESC
		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setText(getItem().getLabelString());
			if (!box.getChildren().isEmpty()) {
				setGraphic(box.getChildren().getFirst());
			}
		}

		@Override
		public void commitEdit(NamedTag tag) {
			if (getItem().parent.getType() == COMPOUND && name != null && name.getText() != null) {
				CompoundTag parent = (CompoundTag) tag.parent;
				if (parent.containsKey(name.getText()) && !name.getText().equals(tag.name)) {
					// don't commit if the new name already exists in this compound tag
					return;
				}
				parent.remove(tag.name);
				parent.put(name.getText(), tag.ref);
				tag.name = name.getText();
			}
			if (value != null && value.getText() != null && !tag.updateValue(value.getText())) {
				value.setText(tag.valueToString());
			}
			super.commitEdit(tag);
		}

		@Override
		public void updateItem(NamedTag tag, boolean empty) {
			super.updateItem(tag, empty);
			setDisclosureNode(null);
			if (empty) {
				setText(null);
				setGraphic(null);
				return;
			}
			ImageView icon = new ImageView(icons[tag.ref.getType().id]);
			setGraphic(icon);
			setText(tag.getLabelString());

			StackPane s = new StackPane();

			Region r = new Region();
			r.setPrefSize(11, 11);
			setMaxSize(11, 11);

			// add the box to every disclosure node for consistent paddings
			s.getChildren().add(r);

			if (tag.isNonEmptyContainerType()) {
				// make box around + / - sign visible with whatever css is defined for .expand-square
				r.getStyleClass().add(expandSquareDisclosureNodeCssClass);

				s.getChildren().addAll(
						horizontal(2, 5, 7, expandDisclosureNodeCssClass), // horizontal line of + or - sign
						horizontal(11, 5, 3) // stubby line to right
				);
				if (!getTreeItem().isExpanded()) {
					// vertical line of + sign
					s.getChildren().add(vertical(5, 2, 7, expandDisclosureNodeCssClass));

					// only show connecting bottom line if this node is not expanded
					if (!((NBTTreeItem) getTreeItem()).isLast()) {
						// connecting line to bottom
						s.getChildren().add(vertical(5, 11, 10));
					}
				}
				if (getTreeItem().getParent() != null) {
					// connecting line to top
					s.getChildren().add(vertical(5, -9, 10));
				}
			} else {
				// connecting line to right
				s.getChildren().add(horizontal(5, 5, 8));

				if (((NBTTreeItem) getTreeItem()).isLast()) {
					// connecting line to top
					s.getChildren().add(vertical(5, -9, 15));
				} else {
					s.getChildren().addAll(
							vertical(5, -9, 15), // vertical line connecting top to middle
							vertical(5, 5, 16) // vertical line connecting middle to bottom
					);
				}
			}

			s.setAlignment(Pos.TOP_LEFT);
			StackPane disclosureNode = new StackPane(s);
			disclosureNode.getStyleClass().add("tree-disclosure-node");
			setDisclosureNode(disclosureNode);
			setEditable(!tag.isRoot());
		}

		private Line horizontal(double tX, double tY, double length, String... id) {
			Line l = new Line(0, 0, length - 1, 0);
			l.setTranslateX(tX);
			l.setTranslateY(tY);
			l.getStyleClass().addAll(id);
			return l;
		}

		private Line vertical(double tX, double tY, double length, String... id) {
			Line l = new Line(0, 0, 0, length - 1);
			l.setTranslateX(tX);
			l.setTranslateY(tY);
			l.getStyleClass().addAll(id);
			return l;
		}

		private TextField nameField() {
			if (name == null) {
				name = new TextField();
				name.getStyleClass().add(nameFieldCssClass);
				HBox.setHgrow(name, Priority.ALWAYS);
				name.setOnKeyPressed(this::onKeyPressed);
			}
			name.setText(getItem().name);
			return name;
		}

		private TextField valueField() {
			if (value == null) {
				value = new TextField();
				value.getStyleClass().add(valueFieldCssClass);
				HBox.setHgrow(value, Priority.ALWAYS);
				value.setOnKeyPressed(this::onKeyPressed);
			}
			value.setText(getItem().valueToString());
			return value;
		}

		private Button editButton() {
			if (edit == null) {
				edit = new Button("edit");
				edit.getStyleClass().add(editButtonCssClass);
			}
			edit.setOnAction(e -> {
				if (arrayEditor != null) {
					Optional<EditArrayResult> result = arrayEditor.apply(getItem().getArray());
					result.ifPresent(r -> getItem().setArray(r.data));
				}
			});
			return edit;
		}

		// event handler to commit an edit when pressing ENTER
		private void onKeyPressed(KeyEvent event) {
			if (event.getCode() == KeyCode.ENTER && isEditing()) {
				commitEdit(getItem());
				updateItem(getItem(), false);
				// prevent parent from handling this event, otherwise it immediately switches to edit mode again
				event.consume();
			}
		}

		private void onDragDetected(MouseEvent event) {
			// can't drag the root node or if the item is null
			if (getItem() == null || getItem().isRoot()) {
				return;
			}

			Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
			WritableImage writableImage = new WritableImage((int) getWidth(), (int) getHeight());
			Image image = snapshot(null, writableImage);
			dragboard.setDragView(image);
			double dX = (noDragviewOffset ? 0 : -getWidth() * 0.5) + event.getX();
			double dY = (noDragviewOffset ? 0 : -getHeight() * 0.5) + event.getY();
			dragboard.setDragViewOffsetX(Math.max(0, Math.min(getWidth(), dX))); // clamp offset to be within the image
			dragboard.setDragViewOffsetY(Math.max(0, Math.min(getHeight(), dY)));
			ClipboardContent clipboardContent = new ClipboardContent();
			clipboardContent.put(NBTTreeItem.clipboardDataformat, true);
			dragboard.setContent(clipboardContent);
			dragItem = (NBTTreeItem) getTreeItem(); // keep track of dragged item separately
			event.consume();
		}

		private void onDragOver(DragEvent event) {
			if (!event.getDragboard().hasContent(NBTTreeItem.clipboardDataformat)) {
				event.consume();
				return;
			}
			event.acceptTransferModes(TransferMode.MOVE);

			// check if we need to scroll up or down
			// calculate whether we're in an area at the top or at the bottom of the treeview using
			// the coordinates of the mouse and the treeview on screen
			double minScreenY = getTreeView().localToScreen(getTreeView().getBoundsInLocal()).getMinY();
			boolean scrollUp = event.getScreenY() < minScreenY + 30;
			boolean scrollDown = event.getScreenY() > minScreenY + getTreeView().getHeight() - 30;
			if (scrollUp || scrollDown) {
				if (verticalScrollBar == null) {
					verticalScrollBar = (ScrollBar) getTreeView().lookup(".scroll-bar:vertical");
				}
				double offset = scrollUp ? -0.02 : 0.02;
				// use Math.min so we don't overshoot, or it will flicker up and down rapidly when scrolling past the bottom
				verticalScrollBar.setValue(Math.min(verticalScrollBar.getValue() + offset, verticalScrollBar.getMax()));
			}

			clearDropTarget();

			// ignore if there's no tree item or if we try to drop an item into itself or one of its children
			if (getTreeItem() == null || isInHierarchy(dragItem) || getItem().ref == dragItem.getValue().parent) {
				event.consume();
				return;
			}

			// ignore if this item is inside a compound tag and we try to drop it into itself
			if (!getItem().isRoot() && !getItem().isContainerType()
					&& getItem().parent.getType() == COMPOUND && getItem().parent == dragItem.getValue().parent) {
				event.consume();
				return;
			}

			// ignore if we try to drop into a list tag that has the wrong type, except if it's an empty list
			if (getItem().ref.getType() == LIST && ((ListTag) getItem().ref).getElementType() != null
					&& ((ListTag) getItem().ref).getElementType() != dragItem.getValue().ref.getType()) {
				event.consume();
				return;
			}

			// when the parent item of the hovered item is a list, we do special highlighting to insert at specific index
			boolean flag = true;
			if (!getItem().isRoot() && getItem().parent.getType() == LIST) {
				flag = false;
				if (((ListTag) getItem().parent).getElementType() != dragItem.getValue().ref.getType()) {
					flag = true;
				} else if (event.getY() > getHeight() * 0.75) {
					dragHighlight((NBTTreeItem) getTreeItem(), 1);
					dropTarget = (NBTTreeItem) getTreeItem().getParent();
					dropTargetIndex = getItem().index + 1;
				} else if (event.getY() < getHeight() * 0.25) {
					dragHighlight((NBTTreeItem) getTreeItem(), -1);
					dropTarget = (NBTTreeItem) getTreeItem().getParent();
					dropTargetIndex = getItem().index;
				} else {
					flag = true;
				}
			}

			// when hovering over a compound or list tag we highlight everything
			if (flag && getItem().isContainerType()) {
				dragHighlight((NBTTreeItem) getTreeItem(), 0);
				dropTarget = (NBTTreeItem) getTreeItem();
				if (getItem().ref.getType() == LIST) {
					dropTargetIndex = ((ListTag) getItem().ref).size();
				}

				// when hovering over a primitive tag inside a compound tag we highlight the parent and its expanded children
			} else if (getItem().parent.getType() == COMPOUND && !getItem().isContainerType()) {
				dragHighlight((NBTTreeItem) getTreeItem().getParent(), 0);
				dropTarget = (NBTTreeItem) getTreeItem().getParent();
			}
		}

		private void onDragDropped(DragEvent event) {
			Dragboard dragboard = event.getDragboard();
			if (!dragboard.hasContent(NBTTreeItem.clipboardDataformat)) {
				event.consume();
				return;
			}

			// no valid target
			if (dropTarget == null) {
				event.consume();
				return;
			}

			dropTarget.moveHere(dragItem, dropTargetIndex);
			clearDropTarget();
		}

		private void onDragExited(DragEvent event) {
			clearDropTarget();
		}

		private void onDragDone(DragEvent event) {
			if (event.getDragboard().hasContent(NBTTreeItem.clipboardDataformat)) {
				dragItem = null;
				clearDropTarget();
			}
		}

		private void clearDropTarget() {
			if (dropHighlight != null) {
				((Group) getParent()).getChildren().remove(dropHighlight);
			}
			dropTarget = null;
			dropTargetIndex = -1;
		}

		private void dragHighlight(NBTTreeItem item, int offset) {
			double yLoc = getLayoutY();
			if (offset > 0) {
				yLoc += getHeight() * 0.5;
			} else if (offset < 0) {
				yLoc -= getHeight() * 0.5;
			} else {
				yLoc -= ((NBTTreeItem) getTreeItem()).getIndexInExpandedHierarchy(item) * getHeight();
			}
			double height = item.countExpandedItems() * getHeight();
			dropHighlight = new Rectangle(getLayoutX(), yLoc, getWidth(), height);
			dropHighlight.getStyleClass().add(dropHighlightCssClass);
			((Group) getParent()).getChildren().addFirst(dropHighlight);
		}

		// checks if source is a parent of this tree item
		private boolean isInHierarchy(NBTTreeItem source) {
			TreeItem<NamedTag> current = getTreeItem();
			while (current != null) {
				if (source == current) {
					return true;
				}
				current = current.getParent();
			}
			return false;
		}
	}

	public record EditArrayResult(Object data) {}
}
