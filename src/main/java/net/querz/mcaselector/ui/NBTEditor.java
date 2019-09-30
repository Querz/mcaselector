package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
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

import java.io.File;
import java.util.Map;
import java.util.Set;

public class NBTEditor extends Dialog<NBTEditor.Result> {

	private CompoundTag data;

	public NBTEditor(TileMap tileMap, Stage primaryStage) {
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("nbt-editor-dialog-pane");
		setResultConverter(p -> p == ButtonType.APPLY ? new Result(data) : null);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

		NBTTreeView nbtTreeView = new NBTTreeView();

		getDialogPane().setContent(nbtTreeView);

		readSingleChunkAsync(tileMap, nbtTreeView);
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
