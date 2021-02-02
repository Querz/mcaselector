package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.CompressionType;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.Chunk;
import net.querz.mcaselector.io.mca.MCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.ui.NBTTreeView;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.nbt.tag.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NBTEditorDialog extends Dialog<NBTEditorDialog.Result> {

	private final Label treeViewPlaceHolder = UIFactory.label(Translation.DIALOG_EDIT_NBT_PLACEHOLDER_LOADING);
	private final TabPane editors = new TabPane();

	private CompoundTag regionData, poiData, entitiesData;
	private final Point2i selectedChunk;

	public NBTEditorDialog(TileMap tileMap, Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_EDIT_NBT_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("nbt-editor-dialog-pane");
		setResultConverter(p -> p == ButtonType.APPLY ? new Result(regionData, poiData, entitiesData) : null);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getScene().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.APPLY).setDisable(true);

		selectedChunk = getSelectedChunk(tileMap);

		getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, e -> {

			Timer t = new Timer();

			DataProperty<Exception> exception = new DataProperty<>();

			new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_SAVING_CHUNK, getDialogPane().getScene().getWindow()).showProgressBar(r -> {
				try {
					r.setMax(4);

					r.updateProgress("region/" + FileHelper.createMCAFileName(selectedChunk.chunkToRegion()), 1);
					writeSingleChunk(FileHelper.createRegionMCAFilePath(selectedChunk.chunkToRegion()), regionData);

					r.incrementProgress("poi/" + FileHelper.createMCAFileName(selectedChunk.chunkToRegion()));
					writeSingleChunk(FileHelper.createPoiMCAFilePath(selectedChunk.chunkToRegion()), poiData);

					r.incrementProgress("entities/" + FileHelper.createMCAFileName(selectedChunk.chunkToRegion()));
					writeSingleChunk(FileHelper.createEntitiesMCAFilePath(selectedChunk.chunkToRegion()), entitiesData);
				} catch (Exception ex) {
					exception.set(ex);
					Debug.dumpException("failed to save chunk", ex);
				} finally {
					r.done("");
				}
			});

			Debug.dumpf("took %s to save chunk %s", t, selectedChunk);

			if (exception.get() != null) {
				e.consume();
				new ErrorDialog(primaryStage, exception.get());
			}
		});

		Tab regionTab = createEditorTab("region", primaryStage, FileHelper.createRegionMCAFilePath(selectedChunk.chunkToRegion()), d -> regionData = d);
		Tab poiTab = createEditorTab("poi", primaryStage, FileHelper.createPoiMCAFilePath(selectedChunk.chunkToRegion()), d -> poiData = d);
		Tab entitiesTab = createEditorTab("entities", primaryStage, FileHelper.createEntitiesMCAFilePath(selectedChunk.chunkToRegion()), d -> entitiesData = d);

		editors.getTabs().addAll(regionTab, poiTab, entitiesTab);

		getDialogPane().setContent(editors);

		Platform.runLater(editors::requestFocus);
	}

	private Point2i getSelectedChunk(TileMap tileMap) {
		Map<Point2i, Set<Point2i>> selection = tileMap.getMarkedChunks();
		if (selection.size() != 1) {
			throw new RuntimeException("only one chunk can be selected, but found selection of " + selection.size() + " regions");
		}
		Point2i location = null;
		for (Map.Entry<Point2i, Set<Point2i>> entry : selection.entrySet()) {
			if (entry.getValue() == null) {
				throw new RuntimeException("only one chunk can be selected, but found entire region " + entry.getKey() + " selected");
			}
			if (entry.getValue().size() != 1) {
				throw new RuntimeException("only one chunk can be selected, but found selection of " + entry.getValue().size() + " chunks");
			}
			for (Point2i p : entry.getValue()) {
				location = p;
			}
		}
		if (location == null) {
			throw new RuntimeException("no selected chunk found");
		}
		return location;
	}

	private Tab createEditorTab(String title, Stage primaryStage, File file, Consumer<CompoundTag> consumer) {
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

		Map<Integer, Label> addTagLabels = new LinkedHashMap<>();

		delete.setOnMouseClicked(e -> {
			if (nbtTreeView.getSelectionModel().getSelectedItem().getParent() == null) {
				consumer.accept(null);
			}
			nbtTreeView.deleteItem(nbtTreeView.getSelectionModel().getSelectedItem());
		});
		nbtTreeView.setOnSelectionChanged((o, n) -> {
			delete.setDisable(n == null);
			enableAddTagLabels(nbtTreeView.getPossibleChildTagTypes(n), addTagLabels);
		});

		BorderPane treeViewHolder = new BorderPane();
		treeViewHolder.getStyleClass().add("nbt-tree-view-holder");
		treeViewHolder.setCenter(treeViewPlaceHolder);
		initAddTagLabels(nbtTreeView, addTagLabels, treeViewHolder, consumer);

		HBox options = new HBox();
		options.getStyleClass().add("nbt-editor-options");
		options.getChildren().add(delete);
		options.getChildren().addAll(addTagLabels.values());

		VBox box = new VBox();
		box.getChildren().addAll(treeViewHolder, options);

		Tab tab = new Tab(title, box);
		tab.setClosable(false);

		readSingleChunkAsync(file, nbtTreeView, treeViewHolder, addTagLabels, consumer);

		return tab;
	}

	private void enableAddTagLabels(int[] ids, Map<Integer, Label> addTagLabels) {
		for (Map.Entry<Integer, Label> label : addTagLabels.entrySet()) {
			label.getValue().setDisable(true);
		}
		if (ids != null) {
			for (int id : ids) {
				addTagLabels.get(id).setDisable(false);
			}
		}
	}

	private void initAddTagLabels(NBTTreeView nbtTreeView, Map<Integer, Label> addTagLabels, BorderPane treeViewHolder, Consumer<CompoundTag> consumer) {
		addTagLabels.put(1, iconLabel("img/nbt/byte", ByteTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(2, iconLabel("img/nbt/short", ShortTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(3, iconLabel("img/nbt/int", IntTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(4, iconLabel("img/nbt/long", LongTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(5, iconLabel("img/nbt/float", FloatTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(6, iconLabel("img/nbt/double", DoubleTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(8, iconLabel("img/nbt/string", StringTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(9, iconLabel("img/nbt/list", () -> ListTag.createUnchecked(EndTag.class), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(10, iconLabel("img/nbt/compound", CompoundTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(7, iconLabel("img/nbt/byte_array", ByteArrayTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(11, iconLabel("img/nbt/int_array", IntArrayTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(12, iconLabel("img/nbt/long_array", LongArrayTag::new, nbtTreeView, treeViewHolder, consumer));
		// disable all add tag labels
		enableAddTagLabels(null, addTagLabels);
	}

	private Label iconLabel(String img, Supplier<Tag<?>> tagSupplier, NBTTreeView nbtTreeView, BorderPane treeViewHolder, Consumer<CompoundTag> consumer) {
		ImageView icon = new ImageView(FileHelper.getIconFromResources(img));
		Label label = new Label("", icon);
		icon.setPreserveRatio(true);
		label.setOnMouseEntered(e -> icon.setFitWidth(18));
		label.setOnMouseExited(e -> icon.setFitWidth(16));
		label.getStyleClass().add("nbt-editor-add-tag-label");
		label.setOnMouseClicked(e -> {
			treeViewHolder.setCenter(nbtTreeView);
			Tag<?> newTag = tagSupplier.get();
			if (nbtTreeView.addItem(nbtTreeView.getSelectionModel().getSelectedItem(), "Unknown", newTag)) {
				// if we created a root tag, it is always a compound tag
				consumer.accept((CompoundTag) newTag);
			}
		});
		return label;
	}

	private void readSingleChunkAsync(File file, NBTTreeView treeView, BorderPane treeViewHolder, Map<Integer, Label> addTagLabels, Consumer<CompoundTag> consumer) {
		new Thread(() -> {
			Debug.dumpf("attempting to read single chunk from file: %s", selectedChunk);
			if (file.exists()) {
				try {
					Chunk chunkData = MCAFile.loadSingleChunk(file, selectedChunk);
					if (chunkData.getData() == null) {
						Debug.dump("no chunk data found for:" + selectedChunk);
						enableAddTagLabels(new int[]{10}, addTagLabels);
						Platform.runLater(() -> treeViewHolder.setCenter(UIFactory.label(Translation.DIALOG_EDIT_NBT_PLACEHOLDER_NO_CHUNK_DATA)));
						return;
					}
					consumer.accept(chunkData.getData());
					Platform.runLater(() -> {
						treeView.setRoot(chunkData.getData());
						treeViewHolder.setCenter(treeView);
						getDialogPane().lookupButton(ButtonType.APPLY).setDisable(false);
					});
				} catch (IOException ex) {
					Debug.dumpException("failed to load chunk", ex);
				}
			} else {
				enableAddTagLabels(new int[]{10}, addTagLabels);
				Platform.runLater(() -> treeViewHolder.setCenter(UIFactory.label(Translation.DIALOG_EDIT_NBT_PLACEHOLDER_NO_REGION_FILE)));
			}
		}).start();
	}

	private void writeSingleChunk(File file, CompoundTag chunkData) throws IOException {
		Chunk chunk = null;
		if (chunkData != null) {
			chunk = new Chunk(selectedChunk);
			chunk.setData(chunkData);
			chunk.setCompressionType(CompressionType.ZLIB);
		}

		try {
			MCAFile.saveSingleChunk(file, selectedChunk, chunk);
			Debug.dumpf("saved single chunk to %s", file);
		} catch (IOException ex) {
			Debug.dumpException("failed to save single chunk to " + file, ex);
			throw ex;
		}
	}

	public static class Result {

		private final CompoundTag regionData, poiData, entitiesData;

		private Result(CompoundTag regionData, CompoundTag poiData, CompoundTag entitiesData) {
			this.regionData = regionData;
			this.poiData = poiData;
			this.entitiesData = entitiesData;
		}

		public CompoundTag getRegionData() {
			return regionData;
		}

		public CompoundTag getPoiData() {
			return poiData;
		}

		public CompoundTag getEntitiesData() {
			return entitiesData;
		}
	}
}
