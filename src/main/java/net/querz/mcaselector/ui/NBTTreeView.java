package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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
import javafx.stage.Stage;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.ui.dialog.EditArrayDialog;
import net.querz.nbt.tag.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class NBTTreeView extends TreeView<NBTTreeView.NamedTag> {

	private static final Map<Byte, Image> icons = new HashMap<>();
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
	}

	public void setOnSelectionChanged(BiConsumer<TreeItem<NamedTag>, TreeItem<NamedTag>> c) {
		selectionChangedAction = c;
		getSelectionModel().selectedItemProperty().addListener((i, o, n) -> c.accept(o, n));
	}

	@SuppressWarnings("unchecked")
	public void deleteItem(TreeItem<NamedTag> item) {
		if (item.getValue().parent != null) {
			// named tag is in compound tag, indexed tag is in list tag
			if (item.getValue().parent.getID() == 10) {
				CompoundTag comp = (CompoundTag) item.getValue().parent;
				comp.remove(item.getValue().name);
				item.getParent().getChildren().remove(item);
			} else if (item.getValue().parent.getID() == 9) {
				ListTag<Tag<?>> list = (ListTag<Tag<?>>) item.getValue().parent;
				int index;
				for (index = 0; index < list.size(); index++) {
					if (list.get(index) == item.getValue().tag) {
						list.remove(index);
						break;
					}
				}
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
	 */
	@SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
	private static void addTagToListTagUnsafe(ListTag<?> list, Tag<?> tag, int index) {
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
				return new int[]{getListTagTypeID((ListTag<?>) target.getValue().tag)};
			}
		}
		// if the tag is a value tag, we lookup the parent
		if (target.getValue().tag.getID() != 9 && target.getValue().tag.getID() != 10) {
			if (target.getParent().getValue().tag.getID() == 9) {
				// when parent is a list tag, we have limited possibilities
				if (((ListTag<?>) target.getParent().getValue().tag).getTypeClass() != EndTag.class) {
					return new int[]{getListTagTypeID((ListTag<?>) target.getParent().getValue().tag)};
				}
			}
		}
		return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
	}

	private static int getListTagTypeID(ListTag<?> listTag) {
		if (listTag.getTypeClass() == EndTag.class) {
			return 0;
		} else if (listTag.getTypeClass() == ByteTag.class) {
			return 1;
		} else if (listTag.getTypeClass() == ShortTag.class) {
			return 2;
		} else if (listTag.getTypeClass() == IntTag.class) {
			return 3;
		} else if (listTag.getTypeClass() == LongTag.class) {
			return 4;
		} else if (listTag.getTypeClass() == FloatTag.class) {
			return 5;
		} else if (listTag.getTypeClass() == DoubleTag.class) {
			return 6;
		} else if (listTag.getTypeClass() == ByteArrayTag.class) {
			return 7;
		} else if (listTag.getTypeClass() == StringTag.class) {
			return 8;
		} else if (listTag.getTypeClass() == ListTag.class) {
			return 9;
		} else if (listTag.getTypeClass() == CompoundTag.class) {
			return 10;
		} else if (listTag.getTypeClass() == IntArrayTag.class) {
			return 11;
		} else if (listTag.getTypeClass() == LongArrayTag.class) {
			return 12;
		} else {
			throw new IllegalArgumentException("invalid list tag type: " + listTag.getTypeClass().getName());
		}
	}

	private static String tagToString(NamedTag tag) {
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
		icons.put((byte) 8, FileHelper.getIconFromResources("img/nbt/string"));
		icons.put((byte) 9, FileHelper.getIconFromResources("img/nbt/list"));
		icons.put((byte) 10, FileHelper.getIconFromResources("img/nbt/compound"));
		icons.put((byte) 7, FileHelper.getIconFromResources("img/nbt/byte_array"));
		icons.put((byte) 11, FileHelper.getIconFromResources("img/nbt/int_array"));
		icons.put((byte) 12, FileHelper.getIconFromResources("img/nbt/long_array"));
	}

	public void setRoot(CompoundTag root) {
		super.setRoot(toTreeItem(null, root, null));
	}

	private static NBTTreeItem toTreeItem(String name, Tag<?> tag, Tag<?> parent) {
		switch (tag.getID()) {
			case 0:
				return null;
			case 9:
				NBTTreeItem item = new NBTTreeItem(new NamedTag(name, tag, parent));
				ListTag<?> list = (ListTag<?>) tag;
				for (int i = 0; i < list.size(); i++) {
					item.getChildren().add(toTreeItem(null, list.get(i), tag));
				}
				return item;
			case 10:
				item = new NBTTreeItem(new NamedTag(name, tag, parent));
				for (Map.Entry<String, Tag<?>> child : (CompoundTag) tag) {
					item.getChildren().add(toTreeItem(child.getKey(), child.getValue(), tag));
				}
				return item;
			default:
				return new NBTTreeItem(new NamedTag(name, tag, parent));
		}
	}

	public static class NamedTag implements Serializable {
		String name;
		Tag<?> tag;
		Tag<?> parent;

		public NamedTag(String name, Tag<?> tag, Tag<?> parent) {
			this.name = name;
			this.tag = tag;
			this.parent = parent;
		}
	}

	public static class NBTTreeItem extends TreeItem<NamedTag> {

		public NBTTreeItem(NamedTag tag) {
			super(tag);
		}

		// removes the item from its previous parent and adds it to this one
		void moveHere(int index, NBTTreeItem item, TreeView<NamedTag> treeView) {
			// do not move if this is a list tag and the types do not match
			if (getValue().tag.getID() == 9) {
				ListTag<?> list = (ListTag<?>) getValue().tag;
				if (list.getTypeClass() != item.getValue().tag.getClass()) {
					return;
				}
			}

			NBTTreeItem oldParent = (NBTTreeItem) item.getParent();

			// do not move if this is item's parent and this are a compoundtag identical
			if (oldParent == this && oldParent.getValue().tag.getID() == 10) {
				return;
			}

			boolean startEdit = false;

			// set name in NamedTag
			if (getValue().tag.getID() == 9) {
				item.getValue().name = null;
			} else if (getValue().tag.getID() == 10) {
				CompoundTag comp = (CompoundTag) getValue().tag;
				if (item.getValue().parent.getID() != 10) {
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

			// add tag to target nbt
			if (getValue().tag.getID() == 9) {
				ListTag<?> list = (ListTag<?>) getValue().tag;
				addTagToListTagUnsafe(list, item.getValue().tag, index);
			} else if (getValue().tag.getID() == 10) {
				CompoundTag comp = (CompoundTag) getValue().tag;
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
		ListTag<?> listTag = (ListTag<?>) list.getValue().tag;
		return listTag.getTypeClass() == EndTag.class || listTag.getTypeClass() == item.getValue().tag.getClass();
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
				if (item.getValue().tag.getID() == 9) {
					// insert before
					if (e.getY() < getHeight() / 4) {
						// if parent is comp, mark comp or top of treeview
						if (item.getParent().getValue().tag.getID() == 10) {
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
							dropIndex = ((ListTag<?>) item.getValue().tag).size();
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
							// if parent is comp, mark comp or top of treeview
							if (item.getParent().getValue().tag.getID() == 10) {
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
							} else if (matchListType(item.getParent(), dragboardContent)) {
								// if parent is list, get index and mark bottom of this cell
								setInsertCssClass("drop-target", "after");
								dropIndex = findDropIndex(item.getParent().getValue().tag, dragboardContent.getValue().tag) + 1;
								dropTarget = item.getParent();
							}
						}
					}
				} else if (item.getValue().tag.getID() == 10) {
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
							// if parent is comp, mark comp or top of treeview
							if (item.getParent().getValue().tag.getID() == 10) {
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
								if (item.getParent().getValue().tag.getID() == 10) {
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
						if (item.getParent().getValue().tag.getID() == 10) {
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
							// if parent is a list, get index and mark top of cell
							setInsertCssClass("drop-target", "before");
							dropIndex = findDropIndex(item.getParent().getValue().tag, dragboardContent.getValue().tag);
							dropTarget = item.getParent();
						}
					} else {
						// insert after
						// if parent is a list
						if (item.getParent().getValue().tag.getID() == 10) {
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

		private int findDropIndex(Tag<?> target, Tag<?> tag) {
			if (target.getID() == 9) {
				ListTag<?> list = (ListTag<?>) target;
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
				// this treecell receives a foreign drop
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

		@SuppressWarnings("unchecked")
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

				if (edit == null) {
					edit = new Button("edit");
					edit.getStyleClass().add("key-value-tree-cell-edit");
				}
				edit.setOnAction(e -> {
					@SuppressWarnings("rawtypes")
					Optional<EditArrayDialog.Result> result = new EditArrayDialog<>(((ArrayTag<?>) getItem().tag).getValue(), stage).showAndWait();
					result.ifPresent(r -> ((ArrayTag<Object>) getItem().tag).setValue(r.getArray()));
				});

				if (box == null) {
					box = new HBox();
					box.setAlignment(Pos.CENTER_LEFT);
				}

				TextField focus = null;


				if (getItem().tag instanceof ArrayTag) {
					if (getItem().name == null) {
						box.getChildren().setAll(getGraphic(), edit);
					} else {
						box.getChildren().setAll(getGraphic(), focus = key, edit);
					}
					edit.requestFocus();
				} else if (getItem().name == null) {
					box.getChildren().setAll(getGraphic(), focus = value);
				} else if (value.getText() == null) {
					box.getChildren().setAll(getGraphic(), focus = key);
				} else if (getItem().name != null && value.getText() != null) {
					box.getChildren().setAll(getGraphic(), key, focus = value);
				} else {
					// cancel if it is a comp or list tag inside a list tag
					return;
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
