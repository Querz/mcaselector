package net.querz.mcaselector.ui;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import net.querz.mcaselector.io.FileHelper;
import net.querz.nbt.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class NBTTreeView extends TreeView<NBTTreeView.NamedTag> {

	private static Map<Byte, Image> icons = new HashMap<>();
	private BiConsumer<TreeItem<NamedTag>, TreeItem<NamedTag>> selectionChangedAction;
	private static final DataFormat CLIPBOARD_DATAFORMAT = new DataFormat("nbt-editor-item");
	private TreeItem<NamedTag> dragboardContent = null;

	static {
		initIcons();
	}

	public NBTTreeView() {
		init();
	}

	public NBTTreeView(CompoundTag root) {
		super(toTreeItem(null, root, null));
		init();
	}

	private void init() {
		setMinWidth(300);
		getStyleClass().add("nbt-tree-view");
		setEditable(true);
		setCellFactory(tv -> new KeyValueTreeCell());
	}

	public void setOnSelectionChanged(BiConsumer<TreeItem<NamedTag>, TreeItem<NamedTag>> c) {
		selectionChangedAction = c;
		getSelectionModel().selectedItemProperty().addListener((i, o, n) -> c.accept(o, n));
	}

	public void deleteItem(TreeItem<NamedTag> item) {
		if (item.getValue().parent != null) {
			// named tag is in compound tag, indexed tag is in list tag
			if (item.getValue().tag.getID() == 10) {
				CompoundTag comp = (CompoundTag) item.getValue().parent;
				comp.remove(item.getValue().name);
				item.getParent().getChildren().remove(item);
			} else if (item.getValue().tag.getID() == 9) {
				@SuppressWarnings("unchecked")
				ListTag<Tag<?>> list = (ListTag<Tag<?>>) item.getValue().parent;
				list.remove(list.indexOf(item.getValue().tag));
				item.getParent().getChildren().remove(item);
			}
		}
	}

	/**
	 * adds a new tag after the target tag.
	 * if the target is not a list or a compound and the parent is a list, it adds the tag on the same level after the target.
	 * if the target is not a list or a compound and the parent is a compound, it adds the tag on the same level.
	 * if the target is a list, it appends the tag as a child at the end of the list.
	 * if the target is a compound, it adds the tag with a generic name.
	 * @param target
	 * @param tag
	 */
	public void addItem(TreeItem<NamedTag> target, String name, Tag<?> tag) {
		TreeItem<NamedTag> newItem = null;
		if (target.getValue().tag.getID() == 9) {
			ListTag<?> list = (ListTag<?>) target.getValue().tag;
			addTagToListTagUnsafe(list, tag, list.size());
			target.getChildren().add(newItem = toTreeItem(null, tag, list));
			target.setExpanded(true);
		} else if (target.getValue().tag.getID() == 10) {
			CompoundTag comp = (CompoundTag) target.getValue().tag;
			name = findNextPossibleName(comp, name);
			comp.put(name, tag);
			target.getChildren().add(newItem = toTreeItem(name, tag, comp));
			target.setExpanded(true);
		} else if (target.getValue().parent.getID() == 9) {
			@SuppressWarnings("unchecked")
			ListTag<Tag<?>> list = (ListTag<Tag<?>>) target.getValue().parent;
			int index;
			for (index = 0; index < list.size(); index++) {
				if (list.get(index) == target.getValue().tag) {
					break;
				}
			}
			addTagToListTagUnsafe(list, tag, index + 1);
			target.getParent().getChildren().add(index + 1, newItem = toTreeItem(null, tag, list));
		} else if (target.getValue().parent.getID() == 10) {
			CompoundTag comp = (CompoundTag) target.getValue().parent;
			name = findNextPossibleName(comp, name);
			comp.put(name, tag);
			target.getParent().getChildren().add(newItem = toTreeItem(name, tag, comp));
		}

		layout();
		getSelectionModel().select(newItem);

		// we don't want to edit this item when the parent is a list tag and it is a list or comp
		if (target.getValue().tag.getID() != 9 && target.getValue().parent != null && target.getValue().parent.getID() != 9 || tag.getID() != 9 && tag.getID() != 10) {
			edit(newItem);
		}


		if (selectionChangedAction != null) {
			selectionChangedAction.accept(getSelectionModel().getSelectedItem(), getSelectionModel().getSelectedItem());
		}
	}

	private static String findNextPossibleName(CompoundTag comp, String name) {
		// if name already exists as a key, add a number to it
		if (comp.containsKey(name)) {
			int num = 1;
			String numName;
			while (comp.containsKey(numName = name + num)){
				num++;
			}
			return numName;
		}
		return name;
	}

	private static void addTagToListTagUnsafe(ListTag<?> list, Tag<?> tag, int index) {
		@SuppressWarnings("unchecked")
		ListTag<Tag<?>> tList = (ListTag<Tag<?>>) list;
		tList.add(index, tag);
	}

	public int[] getPossibleChildTagTypes(TreeItem<NamedTag> target) {
		if (target == null) {
			return null;
		}
		if (target.getValue().tag.getID() == 9) {
			// when this is a list tag, we have limited possibilities
			if (((ListTag<?>) target.getValue().tag).getTypeClass() != EndTag.class) {
				return new int[]{(int) TagFactory.idFromClass(((ListTag<?>) target.getValue().tag).getTypeClass())};
			}
		}
		// if the tag is a value tag, we lookup the parent
		if (target.getValue().tag.getID() != 9 && target.getValue().tag.getID() != 10) {
			if (target.getParent().getValue().tag.getID() == 9) {
				// when parent is a list tag, we have limited possibilities
				if (((ListTag<?>) target.getParent().getValue().tag).getTypeClass() != EndTag.class) {
					return new int[]{(int) TagFactory.idFromClass(((ListTag<?>) target.getParent().getValue().tag).getTypeClass())};
				}
			}
		}
		return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
	}

	private String tagToString(NamedTag tag) {
		switch (tag.tag.getID()) {
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
				return (tag.name == null ? "" : tag.name + ": ") + tag.tag.valueToString(1);
			case 8:
				return (tag.name == null ? "" : tag.name + ": ") + ((StringTag) tag.tag).getValue();
			case 7:
			case 11:
			case 12:
				return (tag.name == null ? "(" : tag.name + " (") + ((ArrayTag<?>) tag.tag).length() + ")";
			case 9:
				return (tag.name == null ? "(" : tag.name + " (") + ((ListTag<?>) tag.tag).size() + ")";
			case 10:
				return (tag.name == null ? "(" : tag.name + " (") + ((CompoundTag) tag.tag).size() + ")";
		}
		return null;
	}

	private String tagValueToString(NamedTag tag) {
		switch (tag.tag.getID()) {
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
				return tag.tag.valueToString(1);
			case 8:
				return ((StringTag) tag.tag).getValue();
			default:
				return null;
		}
	}

	private void updateValue(Tag<?> tag, String value) {
		switch (tag.getID()) {
			case 1:
				((ByteTag) tag).setValue(Byte.parseByte(value));
				return;
			case 2:
				((ShortTag) tag).setValue(Short.parseShort(value));
				return;
			case 3:
				((IntTag) tag).setValue(Integer.parseInt(value));
				return;
			case 4:
				((LongTag) tag).setValue(Long.parseLong(value));
				return;
			case 5:
				((FloatTag) tag).setValue(Float.parseFloat(value));
				return;
			case 6:
				((DoubleTag) tag).setValue(Double.parseDouble(value));
				return;
			case 8:
				((StringTag) tag).setValue(value);
		}
	}

	private static void initIcons() {
		icons.put((byte) 1, FileHelper.getIconFromResources("img/nbt/byte"));
		icons.put((byte) 2, FileHelper.getIconFromResources("img/nbt/short"));
		icons.put((byte) 3, FileHelper.getIconFromResources("img/nbt/int"));
		icons.put((byte) 4, FileHelper.getIconFromResources("img/nbt/long"));
		icons.put((byte) 5, FileHelper.getIconFromResources("img/nbt/float"));
		icons.put((byte) 6, FileHelper.getIconFromResources("img/nbt/double"));
		icons.put((byte) 7, FileHelper.getIconFromResources("img/nbt/byte_array"));
		icons.put((byte) 8, FileHelper.getIconFromResources("img/nbt/string"));
		icons.put((byte) 9, FileHelper.getIconFromResources("img/nbt/list"));
		icons.put((byte) 10, FileHelper.getIconFromResources("img/nbt/compound"));
		icons.put((byte) 11, FileHelper.getIconFromResources("img/nbt/int_array"));
		// TODO: create long array icon
		icons.put((byte) 12, FileHelper.getIconFromResources("img/nbt/int_array"));
	}

	public void setRoot(CompoundTag root) {
		super.setRoot(toTreeItem(null, root, null));
	}

	private static NBTTreeItem toTreeItem(String name, Tag<?> tag, Tag<?> parent) {
		switch (tag.getID()) {
			case 0:
				return null;
			case 10:
				NBTTreeItem item = new NBTTreeItem(new NamedTag(name, tag, parent));
				for (Map.Entry<String, Tag<?>> child : (CompoundTag) tag) {
					item.getChildren().add(toTreeItem(child.getKey(), child.getValue(), tag));
				}
				return item;
			case 9:
				item = new NBTTreeItem(new NamedTag(name, tag, parent));
				ListTag<?> list = (ListTag<?>) tag;
				for (int i = 0; i < list.size(); i++) {
					item.getChildren().add(toTreeItem(null, list.get(i), tag));
				}
				return item;
			default:
				return new NBTTreeItem(new NamedTag(name, tag, parent));
		}
	}

	static class NamedTag implements Serializable {
		String name;
		Tag<?> tag;
		Tag<?> parent;

		public NamedTag(String name, Tag<?> tag, Tag<?> parent) {
			this.name = name;
			this.tag = tag;
			this.parent = parent;
		}
	}

	static class NBTTreeItem extends TreeItem<NamedTag> {

		public NBTTreeItem(NamedTag tag) {
			super(tag);
		}

		// removes the item from its previous parent and adds it to this one
		void moveHere(int index, NBTTreeItem item) {
			// do not move if this is a list tag and the types do not match
			if (getValue().tag.getID() == 9) {
				ListTag<?> list = (ListTag<?>) getValue().tag;
				if (list.getTypeClass() != item.getValue().tag.getClass()) {
					return;
				}
			}

			NBTTreeItem oldParent = (NBTTreeItem) item.getParent();

			// do not move if this if item's parent and this are a compoundtag identical
			if (oldParent == this && oldParent.getValue().tag.getID() == 10) {
				return;
			}

			// remove item from its parent, but remember its index
			int oldIndex;
			for (oldIndex = 0; oldIndex < item.getParent().getChildren().size(); oldIndex++) {
				TreeItem<NamedTag> sourceChild = item.getParent().getChildren().get(oldIndex);
				if (sourceChild == item) {
					sourceChild.getParent().getChildren().remove(oldIndex);
					break;
				}
			}

			// if its parent is this item, we need to adjust the index if it is > i
			if (oldParent == this && oldIndex < index) {
				index--;
			}

			// add source item to this item
			getChildren().add(index, item);


			// now we adjust the backing nbt data

			// remove Tag from source nbt data
			if (item.getValue().parent.getID() == 9) {
				ListTag<?> sourceList = (ListTag<?>) item.getValue().parent;
				for (int i = 0; i < sourceList.size(); i++) {
					if (sourceList.get(i) == item.getValue().tag) {
						sourceList.remove(i);
						break;
					}
				}
			} else if (item.getValue().parent.getID() == 10) {
				CompoundTag comp = (CompoundTag) item.getValue().parent;
				String toRemove = null;
				for (Map.Entry<String, Tag<?>> sourceChild : comp) {
					if (sourceChild.getValue() == item.getValue().tag) {
						toRemove = sourceChild.getKey();
						break;
					}
				}
				comp.remove(toRemove);
			}

			// set new parent in NamedTag
			item.getValue().parent = getValue().tag;

			// set name in NamedTag
			if (getValue().tag.getID() == 9) {
				item.getValue().name = null;
			} else if (getValue().tag.getID() == 10) {
				CompoundTag comp = (CompoundTag) getValue().tag;
				item.getValue().name = findNextPossibleName(comp, "Unknown");
			}

			// add tag to target nbt
			if (getValue().tag.getID() == 9) {
				ListTag<?> list = (ListTag<?>) getValue().tag;
				addTagToListTagUnsafe(list, item.getValue().tag, index);
			} else if (getValue().tag.getID() == 10) {
				CompoundTag comp = (CompoundTag) getValue().tag;
				comp.put(item.getValue().name, item.getValue().tag);
			}
		}

		boolean hasChildWithKey(String key) {
			if (getValue().tag.getID() == 10) {
				CompoundTag comp = (CompoundTag) getValue().tag;
				return comp.containsKey(key);
			}
			return false;
		}
	}

	class KeyValueTreeCell extends TreeCell<NamedTag> {
		private HBox box;
		private TextField key;
		private TextField value;

		KeyValueTreeCell() {
			getStyleClass().add("key-value-tree-cell");
			setOnDragDetected(this::onDragDetected);
			setOnDragOver(this::onDragOver);
			setOnDragDropped(this::onDragDropped);
			setOnDragDone(this::onDragDone);
		}

		private void onDragDetected(MouseEvent e) {
			if (getTreeItem() == getTreeView().getRoot()) {
				return;
			}
			Dragboard db = startDragAndDrop(TransferMode.MOVE);
			WritableImage wi = new WritableImage((int) getWidth(), (int) getHeight());
			Image dbImg = snapshot(null, wi);
			db.setDragView(dbImg);
			ClipboardContent cbc = new ClipboardContent();
			cbc.put(CLIPBOARD_DATAFORMAT, true);
			db.setContent(cbc);
			dragboardContent = getTreeItem();
			e.consume();
		}

		private void onDragOver(DragEvent e) {
			if (e.getDragboard().hasContent(CLIPBOARD_DATAFORMAT)) {
				e.acceptTransferModes(TransferMode.MOVE);

			}
			e.consume();
		}

		private void onDragDropped(DragEvent e) {

			Dragboard db = e.getDragboard();
			if (db.hasContent(CLIPBOARD_DATAFORMAT)) {
				// this treecell receives a foreign drop
				// get content and insert into this tag, before this tag or after this tag
				// and remove dropped tag from old location

				// we also don't want to do anything if the tag is dropped onto itself
				if (getTreeItem() != null && dragboardContent != getTreeItem()) {
					// list: we only want to be able to drop compatible tags into the list
					// if the list is empty (type EndTag) or of the same type as the dragBoard

					/*
					* cases:
					* - drag item into list tag:
					*   - if coming from a compound tag, remove name and add index
					*   - if coming from a list tag, update indices of source list
					*   - update indices of target list
					* - drag item into compound tag
					*   - it is not possible to move a tag inside of the same compound tag
					*   - if coming from a list tag, update indices of source list
					*   - before adding to compound tag, check for name duplicates
					* - move item inside of list tag
					*   - remove item from list and remember previous index
					*   - if the new index is > previous index, subtract 1 from new index
					*   - insert into list and update indices
					*
					* TODO: rearrange lists and comps in a list tag
					*
					* */

					NBTTreeItem from = (NBTTreeItem) dragboardContent;

					// find the item we will move to
					int index = 0;
					NBTTreeItem to;
					if ((getItem().tag.getID() == 9 || getItem().tag.getID() == 10)) {
						System.out.println("into");
						if (e.getY() < getHeight() - getHeight() / 4 && e.getY() > getHeight() / 4) {
							to = (NBTTreeItem) getTreeItem().getParent();
						} else {
							to = (NBTTreeItem) getTreeItem();
						}
					} else {
						System.out.println("same level");
						to = (NBTTreeItem) getTreeItem().getParent();
					}

					if (getItem().tag.getID() == 9) {
						ListTag<?> list = (ListTag<?>) getItem().tag;
						index = list.size();
					} else if (getItem().parent != null && getItem().parent.getID() == 9) {
						ListTag<?> list = (ListTag<?>) getItem().parent;
						for (index = 0; index < list.size(); index++) {
							if (list.get(index) == getItem().tag) {
								break;
							}
						}

						if (e.getY() > getHeight() / 2) {
							index++;
						}
					}

					to.moveHere(index, from);
				}
				dragboardContent = null;
			}
		}

		private void onDragDone(DragEvent e) {
			Dragboard db = e.getDragboard();
			if (db.hasContent(CLIPBOARD_DATAFORMAT)) {
				dragboardContent = null;
			}
		}

		@Override
		public void startEdit() {
			if (!isEditable() || !getTreeView().isEditable()) {
				return;
			}
			super.startEdit();
			if (isEditing()) {
				if (key == null) {
					key = new TextField();
					key.getStyleClass().add("key-value-tree-cell-key");
					key.setOnKeyReleased(this::onKeyReleased);
				}
				key.setText(getItem().name);

				if (value == null) {
					value = new TextField();
					value.getStyleClass().add("key-value-tree-cell-value");
					value.setOnKeyReleased(this::onKeyReleased);
				}
				value.setText(tagValueToString(getItem()));

				if (box == null) {
					box = new HBox();
					box.setAlignment(Pos.CENTER_LEFT);
				}

				if (getItem().name == null && value.getText() == null) {
					return;
				}

				TextField focus;
				if (getItem().name == null) {
					box.getChildren().setAll(getGraphic(), focus = value);
				} else if (value.getText() == null) {
					box.getChildren().setAll(getGraphic(), focus = key);
				} else {
					box.getChildren().setAll(getGraphic(), key, focus = value);
				}
				setGraphic(box);
				setText(null);

				focus.requestFocus();
				focus.selectAll();
			}
		}

		@Override
		public void commitEdit(NamedTag tag) {
			super.commitEdit(tag);
			if (key.getText() != null && !key.getText().isEmpty()) {
				CompoundTag parent = (CompoundTag) tag.parent;
				if (parent.containsKey(key.getText()) && !key.getText().equals(tag.name)) {
					// do not commit if the key cahnged and the key already exists
					return;
				}
				parent.remove(tag.name);
				parent.put(key.getText(), tag.tag);
				tag.name = key.getText();
			}
			if (value.getText() != null) {
				try {
					updateValue(tag.tag, value.getText());
				} catch (Exception ex) {
					// reset text in textfield
					value.setText(tagValueToString(tag));
				}
			}

			System.out.println(getRoot().getValue().tag);
		}

		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setText(tagToString(getItem()));
			if (box.getChildren().size() > 0) {
				setGraphic(box.getChildren().get(0));
			}
		}

		@Override
		public void updateItem(NamedTag tag, boolean empty) {
			super.updateItem(tag, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				setText(tagToString(tag));
				ImageView icon = new ImageView(icons.get(tag.tag.getID()));
				icon.getStyleClass().add("key-value-");
				setGraphic(icon);
				setEditable(tag.parent != null);
			}
		}

		private void onKeyReleased(KeyEvent event) {
			if (event.getCode() == KeyCode.ENTER) {
				commitEdit(getItem());
				updateItem(getItem(), false);
			}
		}
	}

	private void printRoot() {
		printRoot(getRoot(), "");
	}

	private void printRoot(TreeItem<NamedTag> root, String depth) {
		System.out.println(depth + root.getValue().name + " " + tagToString(root.getValue()));
		for (TreeItem<NamedTag> child : root.getChildren()) {
			printRoot(child, depth + "  ");
		}
	}
}
