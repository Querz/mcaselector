package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.text.Translation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DialogHelper {


	public static void showAboutDialog(Stage primaryStage) {
		new AboutDialog(primaryStage).showAndWait();
	}

	public static void changeFields(TileMap tileMap, Stage primaryStage) {
		Optional<ChangeNBTDialog.Result> result = new ChangeNBTDialog(tileMap, primaryStage).showAndWait();
		result.ifPresent(r -> {
			Optional<ButtonType> confRes = new ChangeFieldsConfirmationDialog(null, primaryStage).showAndWait();
			confRes.ifPresent(confR -> {
				if (confR == ButtonType.OK) {
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_CHANGING_NBT_DATA, primaryStage)
							.showProgressBar(t -> FieldChanger.changeNBTFields(
									r.getFields(),
									r.isForce(),
									r.isSelectionOnly() ? tileMap.getMarkedChunks() : null,
									t
							));
				}
			});
		});
	}

	public static void filterChunks(TileMap tileMap, Stage primaryStage) {
		Optional<FilterChunksDialog.Result> result = new FilterChunksDialog(primaryStage).showAndWait();
		result.ifPresent(r -> {
			Debug.dump("chunk filter query: " + r.getFilter());
			if (r.getFilter().isEmpty()) {
				Debug.dump("filter is empty, won't delete everything");
				return;
			}

			switch (r.getType()) {
				case DELETE:
					Optional<ButtonType> confRes = new DeleteConfirmationDialog(null, primaryStage).showAndWait();
					confRes.ifPresent(confR -> {
						if (confR == ButtonType.OK) {
							new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_DELETING_FILTERED_CHUNKS, primaryStage)
									.showProgressBar(t -> ChunkFilterDeleter.deleteFilter(
											r.getFilter(),
											r.isSelectionOnly() ? tileMap.getMarkedChunks() : null,
											t,
											false
									));
							CacheHelper.clearAllCache(tileMap);
						}
					});
					break;
				case EXPORT:
					File dir = createDirectoryChooser(FileHelper.getLastOpenedDirectory("chunk_import_export")).showDialog(primaryStage);
					if (dir != null) {
						confRes = new ExportConfirmationDialog(null, primaryStage).showAndWait();
						confRes.ifPresent(confR -> {
							if (confR == ButtonType.OK) {
								FileHelper.setLastOpenedDirectory("chunk_import_export", dir.getAbsolutePath());
								Debug.dump("exporting chunks to " + dir);
								new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_FILTERED_CHUNKS, primaryStage)
										.showProgressBar(t -> ChunkFilterExporter.exportFilter(
												r.getFilter(),
												r.isSelectionOnly() ? tileMap.getMarkedChunks() : null,
												dir,
												t,
												false
										));
							}
						});
					} else {
						Debug.dump("cancelled exporting chunks, no valid destination directory");
					}
					break;
				case SELECT:
					tileMap.clearSelection();
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_SELECTING_FILTERED_CHUNKS, primaryStage)
							// TODO: radius UI element
							.showProgressBar(t -> ChunkFilterSelector.selectFilter(r.getFilter(), r.getRadius(), selection -> Platform.runLater(() -> {
								tileMap.addMarkedChunks(selection);
								tileMap.update();
							}), t, false));
					break;
				default:
					Debug.dump("i have no idea how you got no selection there...");
			}
		});
	}

	public static void deleteSelection(TileMap tileMap, Stage primaryStage) {
		Optional<ButtonType> result = new DeleteConfirmationDialog(tileMap, primaryStage).showAndWait();
		result.ifPresent(r -> {
			if (r == ButtonType.OK) {
				new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_DELETING_SELECTION, primaryStage)
						.showProgressBar(t -> SelectionDeleter.deleteSelection(tileMap.getMarkedChunks(), t));
				CacheHelper.clearSelectionCache(tileMap);
			}
		});
	}

	public static void exportSelectedChunks(TileMap tileMap, Stage primaryStage) {
		File dir = createDirectoryChooser(FileHelper.getLastOpenedDirectory("chunk_import_export")).showDialog(primaryStage);
		if (dir != null) {
			Optional<ButtonType> result = new ExportConfirmationDialog(tileMap, primaryStage).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					FileHelper.setLastOpenedDirectory("chunk_import_export", dir.getAbsolutePath());
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_SELECTION, primaryStage)
							.showProgressBar(t -> SelectionExporter.exportSelection(tileMap.getMarkedChunks(), dir, t));
				}
			});
		}
	}

	public static void importChunks(TileMap tileMap, Stage primaryStage) {
		File dir = createDirectoryChooser(FileHelper.getLastOpenedDirectory("chunk_import_export")).showDialog(primaryStage);
		DataProperty<ImportConfirmationDialog.ChunkImportConfirmationData> dataProperty = new DataProperty<>();
		if (dir != null) {
			Optional<ButtonType> result = new ImportConfirmationDialog(primaryStage, dataProperty::set).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					FileHelper.setLastOpenedDirectory("chunk_import_export", dir.getAbsolutePath());
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkImporter.importChunks(
									dir, t, false, dataProperty.get().overwrite(),
									dataProperty.get().selectionOnly() ? tileMap.getMarkedChunks() : null,
									dataProperty.get().getOffset()));
					CacheHelper.clearAllCache(tileMap);
				}
			});
		}
	}

	public static void gotoCoordinate(TileMap tileMap, Stage primaryStage) {
		Optional<Point2i> result = new GotoDialog(primaryStage).showAndWait();
		result.ifPresent(r -> tileMap.goTo(r.getX(), r.getY()));
	}

	public static void editSettings(TileMap tileMap, Stage primaryStage) {
		Optional<SettingsDialog.Result> result = new SettingsDialog(primaryStage).showAndWait();
		result.ifPresent(r -> {
			if (Config.getLoadThreads() != r.getReadThreads()
					|| Config.getProcessThreads() != r.getProcessThreads()
					|| Config.getWriteThreads() != r.getWriteThreads()
					|| Config.getMaxLoadedFiles() != r.getMaxLoadedFiles()) {
				MCAFilePipe.init(r.getReadThreads(), r.getProcessThreads(), r.getWriteThreads(), r.getMaxLoadedFiles());
			}

			if (!Config.getLocale().equals(r.getLocale())) {
				Config.setLocale(r.getLocale());
				Locale.setDefault(Config.getLocale());
				Translation.load(Config.getLocale());
			}
			Config.setLoadThreads(r.getReadThreads());
			Config.setProcessThreads(r.getProcessThreads());
			Config.setWriteThreads(r.getWriteThreads());
			Config.setMaxLoadedFiles(r.getMaxLoadedFiles());
			Config.setRegionSelectionColor(new Color(r.getRegionColor()));
			Config.setChunkSelectionColor(new Color(r.getChunkColor()));
			Config.setDebug(r.getDebug());
			tileMap.update();
		});
	}

	public static void editNBT(TileMap tileMap, Stage primaryStage) {
		new NBTEditorDialog(tileMap, primaryStage).showAndWait();
	}

	public static void swapChunks(TileMap tileMap, Stage primaryStage) {
		new ProgressDialog(Translation.MENU_TOOLS_SWAP_CHUNKS, primaryStage).showProgressBar(t -> {
			t.setMax(4);
			Map<Point2i, Set<Point2i>> markedChunks = tileMap.getMarkedChunks();
			ArrayList<Point2i> chunks = new ArrayList<>(2);
			for (Map.Entry<Point2i, Set<Point2i>> entry : markedChunks.entrySet()) {
				chunks.addAll(entry.getValue());
			}
			if (chunks.size() != 2) {
				throw new IllegalStateException("need 2 chunks to swap");
			}

			Point2i fromChunk = chunks.get(0);
			Point2i toChunk = chunks.get(1);
			Point2i fromRegion = fromChunk.chunkToRegion();
			Point2i toRegion = toChunk.chunkToRegion();

			t.incrementProgress(FileHelper.createMCAFileName(fromRegion));

			// load from
			MCAFile fromMCA = loadMCAFileOrCreateEmpty(FileHelper.createMCAFilePath(fromRegion));
			if (fromMCA == null) {
				t.done(null);
				return;
			}

			// load to
			MCAFile toMCA;
			t.incrementProgress(FileHelper.createMCAFileName(toRegion));
			if (fromRegion.equals(toRegion)) {
				toMCA = fromMCA;
			} else {
				toMCA = loadMCAFileOrCreateEmpty(FileHelper.createMCAFilePath(toRegion));
				if (toMCA == null) {
					t.done(null);
					return;
				}
			}

			// from --> to
			Point2i fromOffset = toChunk.sub(fromChunk);
			Point2i toOffset = fromChunk.sub(toChunk);

			MCAChunkData fromData = getLoadedMCAChunkDataOrCreateNew(fromMCA, fromChunk);
			fromData.relocate(fromOffset.chunkToBlock());

			MCAChunkData toData = getLoadedMCAChunkDataOrCreateNew(toMCA, toChunk);
			toData.relocate(toOffset.chunkToBlock());

			fromMCA.setChunkData(fromChunk, toData.getData() == null ? null : toData);
			toMCA.setChunkData(toChunk, fromData.getData() == null ? null : fromData);

			t.incrementProgress(FileHelper.createMCAFileName(toRegion));
			saveMCAFileWithTempFile(toMCA);
			if (fromMCA != toMCA) {
				t.incrementProgress(FileHelper.createMCAFileName(fromRegion));
				saveMCAFileWithTempFile(fromMCA);
			}
			t.done(Translation.DIALOG_PROGRESS_DONE.toString());

			Platform.runLater(() -> CacheHelper.clearSelectionCache(tileMap));
		});
	}

	private static MCAFile loadMCAFileOrCreateEmpty(File file) {
		if (!file.exists()) {
			return new MCAFile(file);
		}
		try {
			return MCAFile.read(file);
		} catch (IOException ex) {
			Debug.error(ex);
		}
		return null;
	}

	private static MCAChunkData getLoadedMCAChunkDataOrCreateNew(MCAFile mcaFile, Point2i location) {
		MCAChunkData loaded = mcaFile.getLoadedChunkData(location);
		if (loaded == null) {
			return mcaFile.getChunkData(location);
		}
		return loaded;
	}

	private static void saveMCAFileWithTempFile(MCAFile mcaFile) {
		try {
			File tmpFrom = File.createTempFile(mcaFile.getFile().getName(), null, null);
			if (mcaFile.save(tmpFrom)) {
				Files.move(tmpFrom.toPath(), mcaFile.getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
			} else {
				tmpFrom.delete();
				mcaFile.getFile().delete();
			}
		} catch (IOException ex) {
			Debug.error(ex);
		}
	}

	public static void openWorld(TileMap tileMap, Stage primaryStage, OptionBar optionBar) {
		String lastOpenDirectory = FileHelper.getLastOpenedDirectory("open_world");
		String savesDir = lastOpenDirectory == null ?  FileHelper.getMCSavesDir() : lastOpenDirectory;
		File file = createDirectoryChooser(savesDir).showDialog(primaryStage);
		if (file != null && file.isDirectory()) {
			File[] files = file.listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.mca$"));
			if (files != null && files.length > 0) {
				Debug.dump("setting world dir to " + file.getAbsolutePath());
				FileHelper.setLastOpenedDirectory("open_world", file.getAbsolutePath());
				Config.setWorldDir(file);
				tileMap.clear();
				tileMap.update();
				tileMap.disable(false);
				optionBar.setWorldDependentMenuItemsEnabled(true);
			}
		}
	}

	public static void importSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(FileHelper.getLastOpenedDirectory("selection_import_export"),
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showOpenDialog(primaryStage);
		if (file != null) {
			Map<Point2i, Set<Point2i>> chunks = SelectionUtil.importSelection(file);
			FileHelper.setLastOpenedDirectory("selection_import_export", file.getParent());
			tileMap.setMarkedChunks(chunks);
			tileMap.update();
		}
	}

	public static void exportSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(FileHelper.getLastOpenedDirectory("selection_import_export"),
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showSaveDialog(primaryStage);
		if (file != null) {
			SelectionUtil.exportSelection(tileMap.getMarkedChunks(), file);
			FileHelper.setLastOpenedDirectory("selection_import_export", file.getParent());
			tileMap.update();
		}
	}

	private static DirectoryChooser createDirectoryChooser(String initialDirectory) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		if (initialDirectory != null) {
			directoryChooser.setInitialDirectory(new File(initialDirectory));
		}
		return directoryChooser;
	}

	private static FileChooser createFileChooser(String initialDirectory, FileChooser.ExtensionFilter filter) {
		FileChooser fileChooser = new FileChooser();
		if (filter != null) {
			fileChooser.getExtensionFilters().add(filter);
		}
		if (initialDirectory != null) {
			fileChooser.setInitialDirectory(new File(initialDirectory));
		}
		return fileChooser;
	}
}
