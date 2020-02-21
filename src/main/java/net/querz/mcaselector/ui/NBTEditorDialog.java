package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.CompressionType;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAChunkData;
import net.querz.mcaselector.io.MCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.TagFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class NBTEditorDialog extends Dialog<NBTEditorDialog.Result> {

	private Map<Integer, Label> addTagLabels = new LinkedHashMap<>();
	private CompoundTag data;
	private Point2i regionLocation;
	private Point2i chunkLocation;

	private BorderPane treeViewHolder = new BorderPane();
	private Label treeViewPlaceHolder = UIFactory.label(Translation.DIALOG_EDIT_NBT_PLACEHOLDER_LOADING);

	public NBTEditorDialog(TileMap tileMap, Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_EDIT_NBT_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("nbt-editor-dialog-pane");
		setResultConverter(p -> p == ButtonType.APPLY ? new Result(data) : null);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.APPLY).setDisable(true);
		((Button) getDialogPane().lookupButton(ButtonType.APPLY)).setOnAction(e -> writeSingleChunk());

		NBTTreeView nbtTreeView = new NBTTreeView(primaryStage);

		ImageView deleteIcon = new ImageView(FileHelper.getIconFromResources("img/delete"));
		Label delete = new Label("", deleteIcon);
		delete.getStyleClass().add("nbt-editor-delete-tag-label");
		delete.setDisable(true);
		deleteIcon.setPreserveRatio(true);
		delete.setOnMouseEntered(e -> {
			if (!delete.isDisabled()) {
				deleteIcon.setFitWidth(24);
			}
		});
		delete.setOnMouseExited(e -> {
			if (!delete.isDisabled()) {
				deleteIcon.setFitWidth(22);
			}
		});
		delete.disableProperty().addListener((i, o, n) -> {
			if (o.booleanValue() != n.booleanValue()) {
				if (n) {
					delete.getStyleClass().remove("nbt-editor-delete-tag-label-enabled");
				} else {
					delete.getStyleClass().add("nbt-editor-delete-tag-label-enabled");
				}
			}
		});

		delete.setOnMouseClicked(e -> nbtTreeView.deleteItem(nbtTreeView.getSelectionModel().getSelectedItem()));
		nbtTreeView.setOnSelectionChanged((o, n) -> {
			delete.setDisable(n == null || n.getParent() == null);
			enableAddTagLabels(nbtTreeView.getPossibleChildTagTypes(n));
		});

		HBox options = new HBox();
		options.getStyleClass().add("nbt-editor-options");

		treeViewHolder.getStyleClass().add("nbt-tree-view-holder");

		initAddTagLabels(nbtTreeView);
		options.getChildren().add(delete);
		options.getChildren().addAll(addTagLabels.values());

		VBox box = new VBox();

		treeViewHolder.setCenter(treeViewPlaceHolder);

		box.getChildren().addAll(treeViewHolder, options);

		getDialogPane().setContent(box);

		readSingleChunkAsync(tileMap, nbtTreeView);
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
		addTagLabels.put(8, iconLabel("img/nbt/string", 8, nbtTreeView));
		addTagLabels.put(9, iconLabel("img/nbt/list", 9, nbtTreeView));
		addTagLabels.put(10, iconLabel("img/nbt/compound", 10, nbtTreeView));
		addTagLabels.put(7, iconLabel("img/nbt/byte_array", 7, nbtTreeView));
		addTagLabels.put(11, iconLabel("img/nbt/int_array", 11, nbtTreeView));
		addTagLabels.put(12, iconLabel("img/nbt/long_array", 12, nbtTreeView));
		// disable all add tag labels
		enableAddTagLabels(null);
	}

	private Label iconLabel(String img, int id, NBTTreeView nbtTreeView) {
		ImageView icon = new ImageView(FileHelper.getIconFromResources(img));
		Label label = new Label("", icon);
		icon.setPreserveRatio(true);
		label.setOnMouseEntered(e -> icon.setFitWidth(18));
		label.setOnMouseExited(e -> icon.setFitWidth(16));
		label.getStyleClass().add("nbt-editor-add-tag-label");
		label.setOnMouseClicked(e -> nbtTreeView.addItem(nbtTreeView.getSelectionModel().getSelectedItem(), "Unknown", TagFactory.fromID(id)));
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
			regionLocation = region.get();
			chunkLocation = chunk.get();
			File file = FileHelper.createMCAFilePath(region.get());
			Debug.dumpf("attempting to read single chunk from file: %s", chunk.get());
			if (file.exists()) {
				MCAChunkData chunkData = MCAFile.readSingleChunk(file, chunk.get());
				if (chunkData == null || chunkData.getData() == null) {
					Debug.dump("no chunk data found for:" + chunk.get());
					Platform.runLater(() -> treeViewHolder.setCenter(UIFactory.label(Translation.DIALOG_EDIT_NBT_PLACEHOLDER_NO_CHUNK_DATA)));
					return;
				}
				data = chunkData.getData();
				Platform.runLater(() -> {
					treeView.setRoot(chunkData.getData());
					treeViewHolder.setCenter(treeView);
					getDialogPane().lookupButton(ButtonType.APPLY).setDisable(false);
				});
			} else {
				Platform.runLater(() -> treeViewHolder.setCenter(UIFactory.label(Translation.DIALOG_EDIT_NBT_PLACEHOLDER_NO_REGION_FILE)));
			}
		}).start();
	}

	private void writeSingleChunk() {
		File file = FileHelper.createMCAFilePath(regionLocation);

		byte[] data = new byte[(int) file.length()];
		try (FileInputStream fis = new FileInputStream(file)) {
			fis.read(data);
		} catch (IOException ex) {
			Debug.error(ex);
			return;
		}

		MCAFile dest = MCAFile.readAll(file, new ByteArrayPointer(data));

		Point2i rel = chunkLocation.mod(32);
		rel.setX(rel.getX() < 0 ? 32 + rel.getX() : rel.getX());
		rel.setY(rel.getY() < 0 ? 32 + rel.getY() : rel.getY());
		int index = rel.getY() * Tile.SIZE_IN_CHUNKS + rel.getX();

		MCAChunkData chunkData = dest.getChunkData(index);
		chunkData.setData(this.data);
		chunkData.setCompressionType(CompressionType.ZLIB);
		dest.setChunkData(index, chunkData);
		dest.setTimeStamp(index, (int) (System.currentTimeMillis() / 1000));
		try {
			File tmpFile = File.createTempFile(file.getName(), null, null);
			try (RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw")) {
				dest.saveAll(raf);
			}
			Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception ex) {
			Debug.error(ex);
		}
	}

	public static class Result {
		private CompoundTag data;

		private Result(CompoundTag data) {
			this.data = data;
		}

		public CompoundTag getData() {
			return data;
		}
	}
}
