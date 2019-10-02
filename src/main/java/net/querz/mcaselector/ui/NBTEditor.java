package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAChunkData;
import net.querz.mcaselector.io.MCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.TagFactory;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NBTEditor extends Dialog<NBTEditor.Result> {

	/*
	* options:
	* delete --> only active when a selection is present
	* add for every tag type --> only active when an item is selected
	*
	* drag-drop from list-tag into list-tag changes indices of all tags in this list-tag
	* drag-drop from comp-tag into list-tag adds index, changes indices of all tags in list-tag and sets name to null
	* drag-drop from list-tag into comp-tag adds default name, changes indices of all tags in list-tag and sets index to null
	*
	* drop on top half of target item: add before
	* drop on bottom half of target item: add after
	*
	* */

	private Map<Integer, Label> addTagLabels = new HashMap<>();

	private CompoundTag data;

	public NBTEditor(TileMap tileMap, Stage primaryStage) {
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("nbt-editor-dialog-pane");
		setResultConverter(p -> p == ButtonType.APPLY ? new Result(data) : null);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

		NBTTreeView nbtTreeView = new NBTTreeView();

		Label delete = new Label();
		delete.setGraphic(new ImageView(FileHelper.getIconFromResources("img/delete")));
		delete.setDisable(true);
		delete.setFocusTraversable(true);
		nbtTreeView.setOnSelectionChanged((o, n) -> {
			delete.setDisable(n == null);
			System.out.println("possible tags to add: " + Arrays.toString(nbtTreeView.getPossibleChildTagTypes(n)));
			enableAddTagLabels(nbtTreeView.getPossibleChildTagTypes(n));
		});

		HBox options = new HBox();

		initAddTagLabels(nbtTreeView);
		options.getChildren().add(delete);
		options.getChildren().addAll(addTagLabels.values());


		VBox box = new VBox();

		box.getChildren().addAll(nbtTreeView, options);

		getDialogPane().setContent(box);

		nbtTreeView.setRoot(data = new CompoundTag());

//		readSingleChunkAsync(tileMap, nbtTreeView);
	}

	private void enableAddTagLabels(int[] ids) {
		for (Map.Entry<Integer, Label> label : addTagLabels.entrySet()) {
			label.getValue().setDisable(true);
		}
		if (ids != null) {
			for (int id : ids) {
				addTagLabels.get(id).setDisable(false);
			}
		}
	}

	private void initAddTagLabels(NBTTreeView nbtTreeView) {
		addTagLabels.put(1, iconLabel("img/nbt/byte", 1, nbtTreeView));
		addTagLabels.put(2, iconLabel("img/nbt/short", 2, nbtTreeView));
		addTagLabels.put(3, iconLabel("img/nbt/int", 3, nbtTreeView));
		addTagLabels.put(4, iconLabel("img/nbt/long", 4, nbtTreeView));
		addTagLabels.put(5, iconLabel("img/nbt/float", 5, nbtTreeView));
		addTagLabels.put(6, iconLabel("img/nbt/double", 6, nbtTreeView));
		addTagLabels.put(7, iconLabel("img/nbt/byte_array", 7, nbtTreeView));
		addTagLabels.put(8, iconLabel("img/nbt/string", 8, nbtTreeView));
		addTagLabels.put(9, iconLabel("img/nbt/list", 9, nbtTreeView));
		addTagLabels.put(10, iconLabel("img/nbt/compound", 10, nbtTreeView));
		addTagLabels.put(11, iconLabel("img/nbt/int_array", 11, nbtTreeView));
		// TODO: create long array icon
		addTagLabels.put(12, iconLabel("img/nbt/int_array", 12, nbtTreeView));
		// disable all add tag labels
		enableAddTagLabels(null);
	}

	private Label iconLabel(String img, int id, NBTTreeView nbtTreeView) {
		ImageView icon = new ImageView(FileHelper.getIconFromResources(img));
		Label label = new Label();
		label.setGraphic(icon);
		label.setOnMouseClicked(e -> {
			nbtTreeView.addItem(nbtTreeView.getSelectionModel().getSelectedItem(), "Unknown", TagFactory.fromID(id));
		});
		return label;
	}

	private void readSingleChunkAsync(TileMap tileMap, NBTTreeView treeView) {
		new Thread(() -> {
			Map<Point2i, Set<Point2i>> selection = tileMap.getMarkedChunks();
			DataProperty<Point2i> region = new DataProperty<>();
			DataProperty<Point2i> chunk = new DataProperty<>();
			selection.forEach((k, v) -> {
				region.set(k);
				v.forEach(chunk::set);
			});
			File file = FileHelper.createMCAFilePath(region.get());
			Debug.dumpf("attempting to read single chunk from file: %s", chunk.get());
			if (file.exists()) {
				MCAChunkData chunkData = MCAFile.readSingleChunk(file, chunk.get());
				if (chunkData == null) {
					Debug.dump("no chunk data found for:" + chunk.get());
					return;
				}
				data = chunkData.getData();
				Platform.runLater(() -> treeView.setRoot(chunkData.getData()));
			}
		}).start();
	}

	public class Result {
		private CompoundTag data;

		private Result(CompoundTag data) {
			this.data = data;
		}

		public CompoundTag getData() {
			return data;
		}
	}
}
