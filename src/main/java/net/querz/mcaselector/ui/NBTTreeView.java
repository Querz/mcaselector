package net.querz.mcaselector.ui;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import net.querz.mcaselector.io.FileHelper;
import net.querz.nbt.*;
import java.util.HashMap;
import java.util.Map;

public class NBTTreeView extends TreeView<NBTTreeView.NamedTag> {

	private static Map<Byte, Image> icons = new HashMap<>();

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

	private static TreeItem<NamedTag> toTreeItem(String name, Tag<?> tag, Tag<?> parent) {
		switch (tag.getID()) {
			case 0:
				return null;
			case 10:
				TreeItem<NamedTag> item = new TreeItem<>(new NamedTag(name, tag, parent));
				for (Map.Entry<String, Tag<?>> child : (CompoundTag) tag) {
					item.getChildren().add(toTreeItem(child.getKey(), child.getValue(), tag));
				}
				return item;
			case 9:
				item = new TreeItem<>(new NamedTag(name, tag, parent));
				for (Tag<?> child : (ListTag<?>) tag) {
					item.getChildren().add(toTreeItem(null, child, tag));
				}
				return item;
			default:
				return new TreeItem<>(new NamedTag(name, tag, parent));
		}
	}

	static class NamedTag {
		String name;
		Tag<?> tag;
		Tag<?> parent;

		public NamedTag(String name, Tag<?> tag, Tag<?> parent) {
			this.name = name;
			this.tag = tag;
			this.parent = parent;
		}
	}

	class KeyValueTreeCell extends TreeCell<NamedTag> {
		private HBox box;
		private TextField key;
		private TextField value;

		KeyValueTreeCell() {
			getStyleClass().add("key-value-tree-cell");
		}

		@Override
		public void startEdit() {
			System.out.println("start edit");
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
					box.getChildren().setAll(getGraphic(), focus = key, value);
				}
				setGraphic(box);
				setText(null);

				focus.requestFocus();
				focus.selectAll();
			}
		}

		@Override
		public void commitEdit(NamedTag tag) {
			System.out.println("commit edit");
			super.commitEdit(tag);
			if (key.getText() != null && !key.getText().isEmpty()) {
				CompoundTag parent = (CompoundTag) tag.parent;
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
		}

		@Override
		public void cancelEdit() {
			System.out.println("cancel edit");
			super.cancelEdit();
			setText(tagToString(getItem()));
			setGraphic(box.getChildren().get(0));
		}

		@Override
		public void updateItem(NamedTag tag, boolean empty) {
			System.out.println("update item");
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

}
