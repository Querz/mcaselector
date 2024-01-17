package net.querz.mcaselector.ui.dialog;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.io.mca.CompressionType;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.Chunk;
import net.querz.mcaselector.io.mca.EntitiesChunk;
import net.querz.mcaselector.io.mca.EntitiesMCAFile;
import net.querz.mcaselector.io.mca.MCAFile;
import net.querz.mcaselector.io.mca.PoiChunk;
import net.querz.mcaselector.io.mca.PoiMCAFile;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.ui.component.NBTTreeView;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.nbt.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static net.querz.nbt.Tag.Type.*;

public class NBTEditorDialog extends Dialog<NBTEditorDialog.Result> {

	private static final Logger LOGGER = LogManager.getLogger(NBTEditorDialog.class);

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
		getDialogPane().getScene().getStylesheets().add(NBTEditorDialog.class.getClassLoader().getResource("style/component/nbt-editor-dialog.css").toExternalForm());
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
					writeSingleChunk(new RegionMCAFile(FileHelper.createRegionMCAFilePath(selectedChunk.chunkToRegion())), new RegionChunk(selectedChunk), regionData);

					r.incrementProgress("poi/" + FileHelper.createMCAFileName(selectedChunk.chunkToRegion()));
					writeSingleChunk(new PoiMCAFile(FileHelper.createPoiMCAFilePath(selectedChunk.chunkToRegion())), new PoiChunk(selectedChunk), poiData);

					r.incrementProgress("entities/" + FileHelper.createMCAFileName(selectedChunk.chunkToRegion()));
					writeSingleChunk(new EntitiesMCAFile(FileHelper.createEntitiesMCAFilePath(selectedChunk.chunkToRegion())), new EntitiesChunk(selectedChunk), entitiesData);
				} catch (Exception ex) {
					exception.set(ex);
					LOGGER.warn("failed to save chunk", ex);
				} finally {
					r.done("");
				}
			});

			LOGGER.warn("took {} to save chunk {}", t, selectedChunk);

			if (exception.get() != null) {
				e.consume();
				new ErrorDialog(primaryStage, exception.get());
			}
		});

		Tab regionTab = createEditorTab("region", primaryStage, new RegionMCAFile(FileHelper.createRegionMCAFilePath(selectedChunk.chunkToRegion())), d -> regionData = d);
		Tab poiTab = createEditorTab("poi", primaryStage, new PoiMCAFile(FileHelper.createPoiMCAFilePath(selectedChunk.chunkToRegion())), d -> poiData = d);
		Tab entitiesTab = createEditorTab("entities", primaryStage, new EntitiesMCAFile(FileHelper.createEntitiesMCAFilePath(selectedChunk.chunkToRegion())), d -> entitiesData = d);

		editors.getTabs().addAll(regionTab, poiTab, entitiesTab);

		getDialogPane().setContent(editors);

		setResizable(true);

		Platform.runLater(editors::requestFocus);
	}

	private Point2i getSelectedChunk(TileMap tileMap) {
		Selection selection = tileMap.getSelection();
		if (selection.size() != 1) {
			throw new RuntimeException("only one chunk can be selected, but found selection of " + selection.size() + " regions");
		}
		Point2i location = null;
		for (Long2ObjectMap.Entry<ChunkSet> entry : selection) {
			if (entry.getValue() == null) {
				throw new RuntimeException("only one chunk can be selected, but found entire region " + new Point2i(entry.getLongKey()) + " selected");
			}
			if (entry.getValue().size() != 1) {
				throw new RuntimeException("only one chunk can be selected, but found selection of " + entry.getValue().size() + " chunks");
			}
			for (int p : entry.getValue()) {
				location = new Point2i(p).add(new Point2i(entry.getLongKey()).regionToChunk());
			}
		}
		if (location == null) {
			throw new RuntimeException("no selected chunk found");
		}
		return location;
	}

	private <T extends Chunk> Tab createEditorTab(String title, Stage primaryStage, MCAFile<T> mcaFile, Consumer<CompoundTag> consumer) {
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

		Map<Tag.Type, Label> addTagLabels = new LinkedHashMap<>();

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
		VBox.setVgrow(treeViewHolder, Priority.ALWAYS);
		box.getChildren().addAll(treeViewHolder, options);

		Tab tab = new Tab(title, box);
		tab.setClosable(false);

		readSingleChunkAsync(mcaFile, nbtTreeView, treeViewHolder, addTagLabels, consumer);

		return tab;
	}

	private void enableAddTagLabels(Tag.Type[] types, Map<Tag.Type, Label> addTagLabels) {
		for (Map.Entry<Tag.Type, Label> label : addTagLabels.entrySet()) {
			label.getValue().setDisable(true);
		}
		if (types != null) {
			for (Tag.Type type : types) {
				addTagLabels.get(type).setDisable(false);
			}
		}
	}

	private void initAddTagLabels(NBTTreeView nbtTreeView, Map<Tag.Type, Label> addTagLabels, BorderPane treeViewHolder, Consumer<CompoundTag> consumer) {
		addTagLabels.put(BYTE, iconLabel("img/nbt/byte", () -> ByteTag.valueOf((byte) 0), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(SHORT, iconLabel("img/nbt/short", () -> ShortTag.valueOf((short) 0), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(INT, iconLabel("img/nbt/int", () -> IntTag.valueOf(0), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(LONG, iconLabel("img/nbt/long", () -> LongTag.valueOf(0), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(FLOAT, iconLabel("img/nbt/float", () -> FloatTag.valueOf(0), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(DOUBLE, iconLabel("img/nbt/double", () -> DoubleTag.valueOf(0), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(STRING, iconLabel("img/nbt/string", () -> StringTag.valueOf(""), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(LIST, iconLabel("img/nbt/list", ListTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(COMPOUND, iconLabel("img/nbt/compound", CompoundTag::new, nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(BYTE_ARRAY, iconLabel("img/nbt/byte_array", () -> new ByteArrayTag(new byte[0]), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(INT_ARRAY, iconLabel("img/nbt/int_array", () -> new IntArrayTag(new int[0]), nbtTreeView, treeViewHolder, consumer));
		addTagLabels.put(LONG_ARRAY, iconLabel("img/nbt/long_array", () -> new LongArrayTag(new long[0]), nbtTreeView, treeViewHolder, consumer));
		// disable all add tag labels
		enableAddTagLabels(null, addTagLabels);
	}

	private Label iconLabel(String img, Supplier<Tag> tagSupplier, NBTTreeView nbtTreeView, BorderPane treeViewHolder, Consumer<CompoundTag> consumer) {
		ImageView icon = new ImageView(FileHelper.getIconFromResources(img));
		Label label = new Label("", icon);
		icon.setPreserveRatio(true);
		label.setOnMouseEntered(e -> icon.setFitWidth(18));
		label.setOnMouseExited(e -> icon.setFitWidth(16));
		label.getStyleClass().add("nbt-editor-add-tag-label");
		label.setOnMouseClicked(e -> {
			treeViewHolder.setCenter(nbtTreeView);
			Tag newTag = tagSupplier.get();
			if (nbtTreeView.addItem(nbtTreeView.getSelectionModel().getSelectedItem(), "Unknown", newTag)) {
				// if we created a root tag, it is always a compound tag
				consumer.accept((CompoundTag) newTag);
			}
		});
		return label;
	}

	private <T extends Chunk> void readSingleChunkAsync(MCAFile<T> mcaFile, NBTTreeView treeView, BorderPane treeViewHolder, Map<Tag.Type, Label> addTagLabels, Consumer<CompoundTag> consumer) {
		new Thread(() -> {
			LOGGER.debug("attempting to read single chunk from file: {}", selectedChunk);
			if (mcaFile.getFile().exists()) {
				try {
					T chunkData = mcaFile.loadSingleChunk(selectedChunk);
					if (chunkData == null || chunkData.getData() == null) {
						LOGGER.debug("no chunk data found for: {}", selectedChunk);
						enableAddTagLabels(new Tag.Type[]{COMPOUND}, addTagLabels);
						Platform.runLater(() -> treeViewHolder.setCenter(UIFactory.label(Translation.DIALOG_EDIT_NBT_PLACEHOLDER_NO_CHUNK_DATA)));
						return;
					}
					consumer.accept(chunkData.getData());
					Platform.runLater(() -> {
						treeView.setRoot(chunkData.getData());
						treeViewHolder.setCenter(treeView);
						treeView.getRoot().setExpanded(true);
						getDialogPane().lookupButton(ButtonType.APPLY).setDisable(false);
					});
				} catch (IOException ex) {
					LOGGER.warn("failed to load chunk from file {}", mcaFile.getFile(), ex);
				}
			} else {
				enableAddTagLabels(new Tag.Type[]{COMPOUND}, addTagLabels);
				Platform.runLater(() -> treeViewHolder.setCenter(UIFactory.label(Translation.DIALOG_EDIT_NBT_PLACEHOLDER_NO_REGION_FILE)));
			}
		}).start();
	}

	private <T extends Chunk> void writeSingleChunk(MCAFile<T> mcaFile, T chunk, CompoundTag chunkData) throws IOException {
		if (chunkData != null) {
			chunk.setData(chunkData);
			chunk.setCompressionType(CompressionType.ZLIB);
		} else {
			chunk = null;
		}

		try {
			mcaFile.saveSingleChunk(selectedChunk, chunk);
			LOGGER.debug("saved single chunk to {}", mcaFile.getFile());
		} catch (IOException ex) {
			LOGGER.warn("failed to save single chunk to {}", mcaFile.getFile(), ex);
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
