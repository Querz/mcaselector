package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.tiles.Selection;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.TileMapSelection;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static net.querz.mcaselector.ui.ImportConfirmationDialog.ChunkImportConfirmationData;

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
					if (r.requiresClearCache()) {
						if (r.isSelectionOnly()) {
							CacheHelper.clearSelectionCache(tileMap);
						} else {
							CacheHelper.clearAllCache(tileMap);
						}
					}
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
							if (r.isSelectionOnly()) {
								CacheHelper.clearSelectionCache(tileMap);
							} else {
								CacheHelper.clearAllCache(tileMap);
							}
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
		DataProperty<ChunkImportConfirmationData> dataProperty = new DataProperty<>();
		if (dir != null) {
			Optional<ButtonType> result = new ImportConfirmationDialog(primaryStage, null, dataProperty::set).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					FileHelper.setLastOpenedDirectory("chunk_import_export", dir.getAbsolutePath());
					DataProperty<Map<Point2i, File>> tempFiles = new DataProperty<>();
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkImporter.importChunks(
									dir, t, false, dataProperty.get().overwrite(),
									null,
									dataProperty.get().selectionOnly() ? tileMap.getMarkedChunks() : null,
									dataProperty.get().getRanges(),
									dataProperty.get().getOffset(),
									tempFiles));
					deleteTempFiles(tempFiles.get());
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
				Config.setLoadThreads(r.getReadThreads());
				Config.setProcessThreads(r.getProcessThreads());
				Config.setWriteThreads(r.getWriteThreads());
				Config.setMaxLoadedFiles(r.getMaxLoadedFiles());
				MCAFilePipe.init();
			}

			if (!Config.getLocale().equals(r.getLocale())) {
				Config.setLocale(r.getLocale());
				Locale.setDefault(Config.getLocale());
				Translation.load(Config.getLocale());
			}
			Config.setRegionSelectionColor(new Color(r.getRegionColor()));
			Config.setChunkSelectionColor(new Color(r.getChunkColor()));
			Config.setPasteChunksColor(new Color(r.getPasteColor()));
			Config.setShade(r.getShade());
			Config.setShadeWater(r.getShadeWater());
			Config.setDebug(r.getDebug());
			tileMap.redrawOverlays();
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

	public static void copySelectedChunks(TileMap tileMap) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Selection selection = new Selection(tileMap.getMarkedChunks(), Config.getWorldDir());
		TileMapSelection tileMapSelection = new TileMapSelection(selection);
		clipboard.setContents(tileMapSelection, tileMap);
	}

	public static void pasteSelectedChunks(TileMap tileMap, Stage primaryStage) {
		if (tileMap.isInPastingMode()) {
			DataProperty<ImportConfirmationDialog.ChunkImportConfirmationData> dataProperty = new DataProperty<>();
			ChunkImportConfirmationData preFill = new ChunkImportConfirmationData(tileMap.getPastedChunksOffset(), true, false, null);
			Optional<ButtonType> result = new ImportConfirmationDialog(primaryStage, preFill, dataProperty::set).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					DataProperty<Map<Point2i, File>> tempFiles = new DataProperty<>();
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkImporter.importChunks(
									tileMap.getPastedWorld(), t, false, dataProperty.get().overwrite(),
									tileMap.getPastedChunks(),
									dataProperty.get().selectionOnly() ? tileMap.getMarkedChunks() : null,
									dataProperty.get().getRanges(),
									dataProperty.get().getOffset(),
									tempFiles));
					deleteTempFiles(tempFiles.get());
					CacheHelper.clearAllCache(tileMap);
				}
			});
		} else {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable content = clipboard.getContents(tileMap);
			DataFlavor[] flavors = content.getTransferDataFlavors();

			if (flavors.length == 1 && flavors[0].equals(TileMapSelection.SELECTION_DATA_FLAVOR)) {
				try {
					Object data = content.getTransferData(flavors[0]);

					Selection selection = (Selection) data;

					tileMap.setPastedChunks(selection.getSelectionData(), selection.getMin(), selection.getMax(), selection.getWorld());
					tileMap.update();

				} catch (UnsupportedFlavorException | IOException ex) {
					Debug.dumpException("failed to paste chunks", ex);
				}
			}
		}
	}

	private static void deleteTempFiles(Map<Point2i, File> tempFiles) {
		if (tempFiles != null) {
			for (File tempFile : tempFiles.values()) {
				if (!tempFile.delete()) {
					Debug.errorf("failed to delete temp file %s", tempFile);
				}
			}
		}
	}

	private static MCAFile loadMCAFileOrCreateEmpty(File file) {
		if (!file.exists()) {
			return new MCAFile(file);
		}
		try {
			return MCAFile.read(file);
		} catch (IOException ex) {
			Debug.dumpException("failed to read MCAFile " + file, ex);
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
			Debug.dumpException(String.format("failed to save MCAFile %s using a temp file", mcaFile.getFile()), ex);
		}
	}

	public static void openWorld(TileMap tileMap, Stage primaryStage, OptionBar optionBar) {
		String lastOpenDirectory = FileHelper.getLastOpenedDirectory("open_world");
		String savesDir = lastOpenDirectory == null ?  FileHelper.getMCSavesDir() : lastOpenDirectory;
		File file = createDirectoryChooser(savesDir).showDialog(primaryStage);
		if (file != null && file.isDirectory()) {
			File[] files = file.listFiles((dir, name) -> name.matches(FileHelper.MCA_FILE_PATTERN));
			if (files != null && files.length > 0) {
				Debug.dump("setting world dir to " + file.getAbsolutePath());
				FileHelper.setLastOpenedDirectory("open_world", file.getAbsolutePath());
				Config.setWorldDir(file);
				CacheHelper.validateCacheVersion(tileMap);
				tileMap.clear();
				tileMap.update();
				tileMap.disable(false);
				optionBar.setWorldDependentMenuItemsEnabled(true, tileMap);
			}
		}
	}

	public static void importSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(FileHelper.getLastOpenedDirectory("selection_import_export"),
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showOpenDialog(primaryStage);
		if (file != null) {
			Map<Point2i, Set<Point2i>> chunks = SelectionHelper.importSelection(file);
			FileHelper.setLastOpenedDirectory("selection_import_export", file.getParent());
			tileMap.setMarkedChunks(chunks);
			tileMap.update();
		}
	}

	public static void exportSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(FileHelper.getLastOpenedDirectory("selection_import_export"),
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showSaveDialog(primaryStage);
		if (file != null) {
			SelectionHelper.exportSelection(tileMap.getMarkedChunks(), file);
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
