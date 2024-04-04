package net.querz.mcaselector.ui.component;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.ui.dialog.EditArrayDialog;
import net.querz.nbt.*;
import static net.querz.nbt.Tag.Type.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NBTTreeView extends TreeView<NBTTreeView.NamedTag> {

	private static final Map<Tag.Type, Image> icons = new HashMap<>();
	private BiConsumer<TreeItem<NamedTag>, TreeItem<NamedTag>> selectionChangedAction;
	private static final DataFormat CLIPBOARD_DATAFORMAT = new DataFormat("nbt-editor-item");
	private TreeItem<NamedTag> dragboardContent = null;
	private TreeItem<NamedTag> dropTarget = null;
	private KeyValueTreeCell dropTargetCell = null;
	private int dropIndex = 0;
	private Stage stage;

	private static final boolean USE_DRAGVIEW_OFFSET;

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		USE_DRAGVIEW_OFFSET = osName.contains("windows");
	}

	static {
		initIcons();
	}

	public NBTTreeView(Stage stage) {
		init(stage);
	}

	private void init(Stage stage) {
		this.stage = stage;
		getStyleClass().add("nbt-tree-view");
		setEditable(true);
		setCellFactory(tv -> new KeyValueTreeCell());
		getStylesheets().add(NBTTreeView.class.getClassLoader().getResource("style/component/nbt-tree-view.css").toExternalForm());
	}

	public void setOnSelectionChanged(BiConsumer<TreeItem<NamedTag>, TreeItem<NamedTag>> c) {
		selectionChangedAction = c;
		getSelectionModel().selectedItemProperty().addListener((i, o, n) -> c.accept(o, n));
	}

	public void deleteItem(TreeItem<NamedTag> item) {
		if (item.getValue().parent != null) {
			// named tag is in compound tag, indexed tag is in list tag
			if (item.getValue().parent instanceof CompoundTag comp) {
				comp.remove(item.getValue().name);
				item.getParent().getChildren().remove(item);
			} else if (item.getValue().parent instanceof ListTag list) {
				for (int index = 0; index < list.size(); index++) {
					if (list.get(index) == item.getValue().tag) {
						list.remove(index);
						break;
					}
				}
				item.getParent().getChildren().remove(item);
			}
		} else {
			setRoot((TreeItem<NamedTag>) null);
		}
	}

	/**
	 * adds a new tag after the target tag.
	 * if the target is not a list or a compound and the parent is a list, it adds the tag on the same level after the target.
	 * if the target is not a list or a compound and the parent is a compound, it adds the tag on the same level.
	 * if the target is a list, it appends the tag as a child at the end of the list.
	 * if the target is a compound, it adds the tag with a generic name.
	 */
	public boolean addItem(TreeItem<NamedTag> target, String name, Tag tag) {
		if (getRoot() == null && tag instanceof CompoundTag) {
			setRoot((CompoundTag) tag);
			layout();
			getSelectionModel().select(0);
			return true;
		}

		TreeItem<NamedTag> newItem = null;
		if (target.getValue().tag instanceof ListTag list) {
			list.add(tag);
			target.getChildren().add(newItem = toTreeItem(list.size() - 1, null, tag, list));
			target.setExpanded(true);
		} else if (target.getValue().tag instanceof CompoundTag comp) {
			name = findNextPossibleName(comp, name);
			comp.put(name, tag);
			target.getChildren().add(newItem = toTreeItem(0, name, tag, comp));
			target.setExpanded(true);
		} else if (target.getValue().parent instanceof ListTag list) {
			int index;
			for (index = 0; index < list.size(); index++) {
				if (list.get(index) == target.getValue().tag) {
					break;
				}
			}
			list.add(index + 1, tag);
			target.getParent().getChildren().add(index + 1, newItem = toTreeItem(index + 1, null, tag, list));
		} else if (target.getValue().parent instanceof CompoundTag comp) {
			name = findNextPossibleName(comp, name);
			comp.put(name, tag);
			target.getParent().getChildren().add(newItem = toTreeItem(0, name, tag, comp));
		}

		layout();
		getSelectionModel().select(newItem);

		// we don't want to edit this item when the parent is a list tag and it is a list or comp
		if (target.getValue().tag.getType() != LIST && target.getValue().parent != null && target.getValue().parent.getType() != LIST || tag.getType() != LIST && tag.getType() != COMPOUND) {
			edit(newItem);
		}

		if (selectionChangedAction != null) {
			selectionChangedAction.accept(getSelectionModel().getSelectedItem(), getSelectionModel().getSelectedItem());
		}

		return false;
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

	public Tag.Type[] getPossibleChildTagTypes(TreeItem<NamedTag> target) {
		if (target == null) {
			if (getRoot() == null) {
				// when there is no root, we give the option to create a root compound tag
				return new Tag.Type[]{COMPOUND};
			}
			return null;
		}
		if (target.getValue().tag instanceof ListTag) {
			// when this is a list tag, we have limited possibilities
			if (((ListTag) target.getValue().tag).getElementType() != null) {
				return new Tag.Type[]{((ListTag) target.getValue().tag).getElementType()};
			}
		}
		// if the tag is a value tag, we lookup the parent
		if (!(target.getValue().tag instanceof ListTag) && !(target.getValue().tag instanceof CompoundTag)) {
			if (target.getParent().getValue().tag instanceof ListTag) {
				// when parent is a list tag, we have limited possibilities
				if (((ListTag) target.getParent().getValue().tag).getElementType() != null) {
					return new Tag.Type[]{((ListTag) target.getParent().getValue().tag).getElementType()};
				}
			}
		}
		return new Tag.Type[]{BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY};
	}

	private static String tagToString(NamedTag tag) {
		return switch (tag.tag.getType()) {
			case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING -> (tag.name == null ? "" : tag.name + ": ") + tagValueToString(tag.tag);
			case LIST, BYTE_ARRAY, INT_ARRAY, LONG_ARRAY -> (tag.name == null ? "(" : tag.name + " (") + ((CollectionTag<?>) tag.tag).size() + ")";
			case COMPOUND -> (tag.name == null ? "(" : tag.name + " (") + ((CompoundTag) tag.tag).size() + ")";
			default -> null;
		};
	}

	private static String tagValueToString(Tag tag) {
		return switch (tag.getType()) {
			case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> ((NumberTag) tag).asNumber().toString();
			case STRING -> ((StringTag) tag).getValue();
			default -> null;
		};
	}

	private void updateValue(Tag parent, NamedTag tag, String value) {
		Consumer<Tag> setter = newTag -> {
			switch (parent.getType()) {
				case LIST -> ((ListTag) parent).set(tag.index, newTag);
				case COMPOUND -> ((CompoundTag) parent).put(tag.name, newTag);
			}
			tag.tag = newTag;
		};

		switch (tag.tag.getType()) {
			case BYTE -> setter.accept(ByteTag.valueOf(Byte.parseByte(value)));
			case SHORT -> setter.accept(ShortTag.valueOf(Short.parseShort(value)));
			case INT -> setter.accept(IntTag.valueOf(Integer.parseInt(value)));
			case LONG -> setter.accept(LongTag.valueOf(Long.parseLong(value)));
			case FLOAT -> setter.accept(FloatTag.valueOf(Float.parseFloat(value)));
			case DOUBLE -> setter.accept(DoubleTag.valueOf(Double.parseDouble(value)));
			case STRING -> setter.accept(StringTag.valueOf(value));
		}
	}

	private static void initIcons() {
		icons.put(BYTE, FileHelper.getIconFromResources("img/nbt/byte"));
		icons.put(SHORT, FileHelper.getIconFromResources("img/nbt/short"));
		icons.put(INT, FileHelper.getIconFromResources("img/nbt/int"));
		icons.put(LONG, FileHelper.getIconFromResources("img/nbt/long"));
		icons.put(FLOAT, FileHelper.getIconFromResources("img/nbt/float"));
		icons.put(DOUBLE, FileHelper.getIconFromResources("img/nbt/double"));
		icons.put(STRING, FileHelper.getIconFromResources("img/nbt/string"));
		icons.put(LIST, FileHelper.getIconFromResources("img/nbt/list"));
		icons.put(COMPOUND, FileHelper.getIconFromResources("img/nbt/compound"));
		icons.put(BYTE_ARRAY, FileHelper.getIconFromResources("img/nbt/byte_array"));
		icons.put(INT_ARRAY, FileHelper.getIconFromResources("img/nbt/int_array"));
		icons.put(LONG_ARRAY, FileHelper.getIconFromResources("img/nbt/long_array"));
	}

	public void setRoot(CompoundTag root) {
		super.setRoot(toTreeItem(0, null, root, null));
	}

	private static NBTTreeItem toTreeItem(int index, String name, Tag tag, Tag parent) {
		switch (tag.getType()) {
			case END:
				return null;
			case LIST:
				NBTTreeItem item = new NBTTreeItem(new NamedTag(index, name, tag, parent));
				ListTag list = (ListTag) tag;
				for (int i = 0; i < list.size(); i++) {
					item.getChildren().add(toTreeItem(i, null, list.get(i), tag));
				}
				return item;
			case COMPOUND:
				item = new NBTTreeItem(new NamedTag(index, name, tag, parent));
				for (Map.Entry<String, Tag> child : (CompoundTag) tag) {
					item.getChildren().add(toTreeItem(0, child.getKey(), child.getValue(), tag));
				}
				return item;
			default:
				return new NBTTreeItem(new NamedTag(index, name, tag, parent));
		}
	}

	public static class NamedTag implements Serializable {
		int index;
		String name;
		Tag tag;
		Tag parent;

		public NamedTag(int index, String name, Tag tag, Tag parent) {
			this.index = index;
			this.name = name;
			this.tag = tag;
			this.parent = parent;
		}

		@Override
		public String toString() {
			return "i=" + index + ", n=" + name + ", t=" + tag.getType() + ", p=" + (parent == null ? "-" : parent.getType()) + ", v=" + NBTUtil.toSNBT(tag);
		}
	}

	public static class NBTTreeItem extends TreeItem<NamedTag> {

		public NBTTreeItem(NamedTag tag) {
			super(tag);
		}

		// removes the item from its previous parent and adds it to this one
		void moveHere(int index, NBTTreeItem item, TreeView<NamedTag> treeView) {
			// do not move if this is a list tag and the types do not match
			if (getValue().tag instanceof ListTag list) {
				if (list.getElementType() != item.getValue().tag.getType()) {
					return;
				}
			}

			NBTTreeItem oldParent = (NBTTreeItem) item.getParent();

			// do not move if this is item's parent and this are a compound tag identical
			if (oldParent == this && oldParent.getValue().tag instanceof CompoundTag) {
				return;
			}

			boolean startEdit = false;

			// set name in NamedTag
			if (getValue().tag instanceof ListTag) {
				item.getValue().name = null;
			} else if (getValue().tag instanceof CompoundTag comp) {
				if (item.getValue().parent.getType() != COMPOUND) {
					item.getValue().name = findNextPossibleName(comp, "Unknown");
					startEdit = true;
				}
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
			if (item.getValue().parent instanceof ListTag sourceList) {
				for (int i = 0; i < sourceList.size(); i++) {
					if (sourceList.get(i) == item.getValue().tag) {
						sourceList.remove(i);
						break;
					}
				}
			} else if (item.getValue().parent instanceof CompoundTag comp) {
				String toRemove = null;
				for (Map.Entry<String, Tag> sourceChild : comp) {
					if (sourceChild.getValue() == item.getValue().tag) {
						toRemove = sourceChild.getKey();
						break;
					}
				}
				comp.remove(toRemove);
			}

			// set new parent in NamedTag
			item.getValue().parent = getValue().tag;

			// add tag to target nbt
			if (getValue().tag instanceof ListTag list) {
				list.add(index, item.getValue().tag);
			} else if (getValue().tag instanceof CompoundTag comp) {
				comp.put(item.getValue().name, item.getValue().tag);
			}

			if (startEdit) {
				treeView.layout();
				treeView.scrollTo(treeView.getRow(item));
				treeView.getSelectionModel().select(item);
				Platform.runLater(() -> treeView.edit(item));
			}
		}
	}

	public KeyValueTreeCell getTreeCell(TreeItem<NamedTag> treeItem) {
		return recursiveFindCellByItem(treeItem, this);
	}

	private KeyValueTreeCell recursiveFindCellByItem(TreeItem<?> treeItem, Node node) {
		if (node.getStyleClass().contains("key-value-tree-cell")
				&& KeyValueTreeCell.class.isAssignableFrom(node.getClass())
				&& ((KeyValueTreeCell) node).getTreeItem() == treeItem) {
			return (KeyValueTreeCell) node;
		}

		if (!Parent.class.isAssignableFrom(node.getClass())) {
			return null;
		}

		List<Node> nodes = ((Parent) node).getChildrenUnmodifiable();
		if (nodes == null) {
			return null;
		}

		for (Node n : nodes) {
			KeyValueTreeCell cell = recursiveFindCellByItem(treeItem, n);
			if (cell != null) {
				return cell;
			}
		}
		return null;
	}

	private boolean matchListType(TreeItem<NamedTag> list, TreeItem<NamedTag> item) {
		ListTag listTag = (ListTag) list.getValue().tag;
		return listTag.getElementType() == null || listTag.getElementType() == item.getValue().tag.getType();
	}

	class KeyValueTreeCell extends TreeCell<NamedTag> {
		private HBox box;
		private TextField key;
		private TextField value;
		private Button edit;

		KeyValueTreeCell() {
			getStyleClass().add("key-value-tree-cell");
			setOnDragDetected(this::onDragDetected);
			setOnDragOver(this::onDragOver);
			setOnDragDropped(this::onDragDropped);
			setOnDragDone(this::onDragDone);
			setOnDragExited(e -> setOnDragExited());
		}

		private void onDragDetected(MouseEvent e) {
			if (getTreeItem() == getTreeView().getRoot()) {
				return;
			}
			Dragboard db = startDragAndDrop(TransferMode.MOVE);
			WritableImage wi = new WritableImage((int) getWidth(), (int) getHeight());
			Image dbImg = snapshot(null, wi);
			db.setDragView(dbImg);
			if (USE_DRAGVIEW_OFFSET) {
				db.setDragViewOffsetX(getWidth() / 2);
				db.setDragViewOffsetY(getHeight() / 2);
			}
			ClipboardContent cbc = new ClipboardContent();
			cbc.put(CLIPBOARD_DATAFORMAT, true);
			db.setContent(cbc);
			dragboardContent = getTreeItem();
			e.consume();
		}

		private void onDragOver(DragEvent e) {
			if (e.getDragboard().hasContent(CLIPBOARD_DATAFORMAT)) {
				e.acceptTransferModes(TransferMode.MOVE);

				clearMarkings();

				// if there is no tree item or if we try to insert the item as a child of itself, stop here
				if (getTreeItem() == null || isInHierarchy(dragboardContent)) {
					e.consume();
					return;
				}

				// if target is a list or a comp
				NBTTreeItem item = (NBTTreeItem) getTreeItem();
				// move into list
				if (item.getValue().tag instanceof ListTag) {
					// insert before
					if (e.getY() < getHeight() / 4) {
						// if parent is comp, mark comp or top of tree view
						if (item.getParent().getValue().tag instanceof CompoundTag) {
							// if parent is equal to the dragged item's parent
							if (item.getParent() != dragboardContent.getParent()) {
								KeyValueTreeCell cell = getTreeCell(item.getParent());
								if (cell == null) {
									// mark top of tree view
									dropTarget = item.getParent();
									setInsertParentCss(true);
								} else {
									// mark comp item
									cell.setInsertCssClass("drop-target", "into");
									dropTarget = item.getParent();
									setDropTargetCell(cell);
								}
							}
						} else if (matchListType(item.getParent(), dragboardContent)) {
							// if parent is list, get index and mark top of this cell
							setInsertCssClass("drop-target", "before");
							dropIndex = findDropIndex(item.getParent().getValue().tag, dragboardContent.getValue().tag);
							dropTarget = item.getParent();
						}
					} else if (e.getY() < getHeight() - getHeight() / 4) {
						// check if we can insert this tag into this list
						if (matchListType(item, dragboardContent)) {
							dropTarget = item;
							setInsertCssClass("drop-target", "into");
							dropIndex = ((ListTag) item.getValue().tag).size();
						}
					} else {
						// insert after or at beginning of list
						// insert at index 0 of list
						if (item.isExpanded()) {
							if (matchListType(item, dragboardContent)) {
								// insert into this list
								dropTarget = item;
								dropIndex = 0;
								setInsertCssClass("drop-target", "after");
							}
						} else {
							// insert after this list in parent
							// if parent is comp, mark comp or top of tree view
							if (item.getParent().getValue().tag instanceof CompoundTag) {
								// if parent is equal to the dragged item's parent
								if (item.getParent() != dragboardContent.getParent()) {
									KeyValueTreeCell cell = getTreeCell(item.getParent());
									if (cell == null) {
										// mark top of tree view
										dropTarget = item.getParent();
										setInsertParentCss(true);
									} else {
										// mark comp item
										cell.setInsertCssClass("drop-target", "into");
										dropTarget = item.getParent();
										setDropTargetCell(cell);
									}
								}
							} else if (matchListType(item.getParent(), dragboardContent)) {
								// if parent is list, get index and mark bottom of this cell
								setInsertCssClass("drop-target", "after");
								dropIndex = findDropIndex(item.getParent().getValue().tag, dragboardContent.getValue().tag) + 1;
								dropTarget = item.getParent();
							}
						}
					}
				} else if (item.getValue().tag instanceof CompoundTag) {
					// if this tag is a comp
					// if target is the root tag
					if (item.getParent() == null) {
						if (item != dragboardContent.getParent()) {
							setInsertCssClass("drop-target", "into");
							setDropTargetCell(this);
							dropTarget = item;
						}
					} else {
						// insert before
						if (e.getY() < getHeight() / 4) {
							// if parent is comp, mark comp or top of tree view
							if (item.getParent().getValue().tag instanceof CompoundTag) {
								// if parent is equal to the dragged item's parent
								if (item.getParent() != dragboardContent.getParent()) {
									KeyValueTreeCell cell = getTreeCell(item.getParent());
									if (cell == null) {
										// mark top of tree view
										dropTarget = item.getParent();
										setInsertParentCss(true);
									} else {
										// mark comp item
										cell.setInsertCssClass("drop-target", "into");
										setDropTargetCell(cell);
										dropTarget = item.getParent();
									}
								}
							} else if (matchListType(item.getParent(), dragboardContent)) {
								// if parent is list, get index and mark top of this cell
								setInsertCssClass("drop-target", "before");
								dropIndex = findDropIndex(item.getParent().getValue().tag, dragboardContent.getValue().tag);
								dropTarget = item.getParent();
							}
						} else if (e.getY() < getHeight() - getHeight() / 4) {
							// insert into
							// if parent is equal to the dragged item's parent
							if (item != dragboardContent.getParent()) {
								dropTarget = item;
								setInsertCssClass("drop-target", "into");
								setDropTargetCell(this);
							}
						} else {
							// insert after
							// if parent is equal to the dragged item's parent
							if (item != dragboardContent.getParent()) {
								// if parent is a comp
								if (item.getParent().getValue().tag instanceof CompoundTag) {
									// if parent is equal to the dragged item's parent
									if (item.getParent() != dragboardContent.getParent()) {
										KeyValueTreeCell cell = getTreeCell(item.getParent());
										if (cell == null) {
											// mark top of tree view
											dropTarget = item.getParent();
											setInsertParentCss(true);
										} else {
											// mark comp item
											cell.setInsertCssClass("drop-target", "into");
											dropTarget = item.getParent();
											setDropTargetCell(cell);
										}
									}
								} else if (matchListType(item.getParent(), dragboardContent)) {
									// if parent is list, get index and mark bottom of this cell
									setInsertCssClass("drop-target", "after");
									dropIndex = findDropIndex(item.getParent().getValue().tag, dragboardContent.getValue().tag) + 1;
									dropTarget = item.getParent();
								}
							}
						}
					}
				} else {
					// if target is neither a list nor a comp
					// insert before
					if (e.getY() < getHeight() / 2) {
						// if parent is a list
						if (item.getParent().getValue().tag instanceof CompoundTag) {
							// if parent is equal to the dragged item's parent
							if (item.getParent() != dragboardContent.getParent()) {
								KeyValueTreeCell cell = getTreeCell(item.getParent());
								if (cell == null) {
									// mark top of tree view
									dropTarget = item.getParent();
									setInsertParentCss(true);
								} else {
									// mark comp item
									cell.setInsertCssClass("drop-target", "into");
									dropTarget = item.getParent();
									setDropTargetCell(cell);
								}
							}
						} else if (matchListType(item.getParent(), dragboardContent)){
							// if parent is a list, get index and mark top of cell
							setInsertCssClass("drop-target", "before");
							dropIndex = findDropIndex(item.getParent().getValue().tag, dragboardContent.getValue().tag);
							dropTarget = item.getParent();
						}
					} else {
						// insert after
						// if parent is a list
						if (item.getParent().getValue().tag instanceof CompoundTag) {
							// if parent is equal to the dragged item's parent
							if (item.getParent() != dragboardContent.getParent()) {
								KeyValueTreeCell cell = getTreeCell(item.getParent());
								if (cell == null) {
									// mark top of treeview
									dropTarget = item.getParent();
									setInsertParentCss(true);
								} else {
									// mark comp item
									cell.setInsertCssClass("drop-target", "into");
									dropTarget = item.getParent();
									setDropTargetCell(cell);
								}
							}
						} else if (matchListType(item.getParent(), dragboardContent)){
							// if parent is a list, get index and mark bottom of cell
							setInsertCssClass("drop-target", "after");
							dropIndex = findDropIndex(item.getParent().getValue().tag, dragboardContent.getValue().tag) + 1;
							dropTarget = item.getParent();
						}
					}
				}
			}
			e.consume();
		}

		private boolean isInHierarchy(TreeItem<NamedTag> source) {
			TreeItem<NamedTag> current = getTreeItem();
			while (current != null) {
				if (source == current) {
					return true;
				}
				current = current.getParent();
			}
			return false;
		}

		private void clearMarkings() {
			setInsertParentCss(false);
			setInsertCssClass("drop-target", null);
			setDropTargetCell(null);
			dropTarget = null;
		}

		private void setOnDragExited() {
			clearMarkings();
		}

		private void setDropTargetCell(KeyValueTreeCell cell) {
			if (dropTargetCell != null && dropTargetCell != cell) {
				dropTargetCell.setInsertCssClass("drop-target", null);
			}
			dropTargetCell = cell;
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
					setInsertParentCss(false);
					return;
				}
			}
			getStyleClass().add(c);
			setInsertParentCss(false);
		}

		private int findDropIndex(Tag target, Tag tag) {
			if (target instanceof ListTag list) {
				for (int index = 0; index < list.size(); index++) {
					if (list.get(index) == tag) {
						return index;
					}
				}
			}
			return 0;
		}

		private void onDragDropped(DragEvent e) {
			Dragboard db = e.getDragboard();
			if (db.hasContent(CLIPBOARD_DATAFORMAT)) {
				// this tree cell receives a foreign drop
				// get content and insert into this tag, before this tag or after this tag
				// and remove dropped tag from old location

				// we also don't want to do anything if the tag is dropped onto itself or if the target is invalid
				if (getTreeItem() != null && dragboardContent != getTreeItem() && dropTarget != null) {
					((NBTTreeItem) dropTarget).moveHere(dropIndex, (NBTTreeItem) dragboardContent, getTreeView());
				}
				dragboardContent = null;
			}
		}

		private void onDragDone(DragEvent e) {
			Dragboard db = e.getDragboard();
			if (db.hasContent(CLIPBOARD_DATAFORMAT)) {
				dragboardContent = null;
			}

			if (dropTargetCell != null) {
				dropTargetCell.setInsertCssClass("drop-target", null);
			}
			setInsertParentCss(false);
		}

		private static Object tagToArray(Tag tag) {
			return switch (tag.getType()) {
				case BYTE_ARRAY -> ((ByteArrayTag) tag).getValue();
				case INT_ARRAY -> ((IntArrayTag) tag).getValue();
				case LONG_ARRAY -> ((LongArrayTag) tag).getValue();
				default -> null;
			};
		}

		private static void setArrayValue(Tag tag, Object array) {
			switch (tag.getType()) {
				case BYTE_ARRAY -> ((ByteArrayTag) tag).setValue((byte[]) array);
				case INT_ARRAY -> ((IntArrayTag) tag).setValue((int[]) array);
				case LONG_ARRAY -> ((LongArrayTag) tag).setValue((long[]) array);
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
				value.setText(tagValueToString(getItem().tag));

				if (edit == null) {
					edit = new Button("edit");
					edit.getStyleClass().add("key-value-tree-cell-edit");
				}
				edit.setOnAction(e -> {
					@SuppressWarnings("rawtypes")
					Optional<EditArrayDialog.Result> result = new EditArrayDialog<>(tagToArray(getItem().tag), stage).showAndWait();
					result.ifPresent(r -> setArrayValue(getItem().tag, r.getArray()));
				});

				if (box == null) {
					box = new HBox();
					box.setAlignment(Pos.CENTER_LEFT);
				}

				TextField focus = null;

				if (getItem().parent != null) {
					if (getItem().parent instanceof CompoundTag) {
						if (getItem().tag.getType() == BYTE_ARRAY || getItem().tag.getType() == INT_ARRAY || getItem().tag.getType() == LONG_ARRAY) {
							// array inside compound: name + edit
							box.getChildren().setAll(getGraphic(), focus = key, edit);
						} else if (getItem().tag instanceof CompoundTag || getItem().tag instanceof ListTag) {
							// container inside compound: name
							box.getChildren().setAll(getGraphic(), focus = key);
						} else {
							// rest inside compound: name + value
							box.getChildren().setAll(getGraphic(), focus = key, value);
						}
					} else if (getItem().parent instanceof ListTag) {
						if (getItem().tag.getType() == BYTE_ARRAY || getItem().tag.getType() == INT_ARRAY || getItem().tag.getType() == LONG_ARRAY) {
							// array inside list: edit
							box.getChildren().setAll(getGraphic(), edit);
						} else if (getItem().tag instanceof CompoundTag || getItem().tag instanceof ListTag) {
							// container inside list: not editable
							return;
						} else {
							// rest inside list: value
							box.getChildren().setAll(getGraphic(), focus = value);
						}
					}
				}

				setGraphic(box);
				setText(null);

				if (focus != null) {
					focus.requestFocus();
					focus.selectAll();
				}
			}
		}

		@Override
		public void commitEdit(NamedTag tag) {
			if (key.getText() != null && !key.getText().isEmpty()) {
				CompoundTag parent = (CompoundTag) tag.parent;
				if (parent.containsKey(key.getText()) && !key.getText().equals(tag.name)) {
					// do not commit if the key changed and the key already exists
					return;
				}
				parent.remove(tag.name);
				parent.put(key.getText(), tag.tag);
				tag.name = key.getText();
			}
			if (value.getText() != null) {
				try {
					updateValue(tag.parent, tag, value.getText());
				} catch (Exception ex) {
					// reset text in text field
					value.setText(tagValueToString(tag.tag));
				}
			}
			super.commitEdit(tag);
		}

		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setText(tagToString(getItem()));
			if (!box.getChildren().isEmpty()) {
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
				ImageView icon = new ImageView(icons.get(tag.tag.getType()));
				String lineChar;
				if (tag.parent == null) {
					 lineChar = "╶";
				} else if (getTreeItem().isExpanded() || isLastItem(tag)) {
					lineChar = "└";
				} else {
					lineChar = "├";
				}
				Label lc = new Label(lineChar);
				lc.getStyleClass().add("line-char");
				StackPane sp;
				if (tag.tag.getType() == LIST && !((ListTag) tag.tag).isEmpty() || tag.tag.getType() == COMPOUND && !((CompoundTag) tag.tag).isEmpty()) {
					String expand;
					if (getTreeItem().isExpanded()) {
						expand = "-";
					} else {
						expand = "+";
					}
					Label cellExpand = new Label(expand);
					cellExpand.getStyleClass().add("cell-expand");
					sp = new StackPane(lc, cellExpand);
					sp.getStyleClass().add("line-node");
				} else {
					sp = new StackPane(lc);
				}
				HBox graphic = new HBox(sp, icon);
				graphic.getStyleClass().add("cell-graphic");
				setGraphic(graphic);
				setText(tagToString(tag));
				setEditable(tag.parent != null);
			}
		}

		private boolean isLastItem(NamedTag tag) {
			if (tag.parent instanceof CompoundTag || tag.parent instanceof ListTag) {
				ObservableList<TreeItem<NamedTag>> childrenItems = getTreeItem().getParent().getChildren();
				return childrenItems.get(childrenItems.size() - 1).getValue() == tag;
			}
			return false;
		}

		private void onKeyReleased(KeyEvent event) {
			if (event.getCode() == KeyCode.ENTER) {
				commitEdit(getItem());
				updateItem(getItem(), false);
			}
		}
	}

	private void setInsertParentCss(boolean enabled) {
		if (enabled) {
			if (getStyleClass().contains("nbt-tree-view-drop-parent")) {
				return;
			}
			getStyleClass().add("nbt-tree-view-drop-parent");
		} else {
			getStyleClass().remove("nbt-tree-view-drop-parent");
		}
	}
}
