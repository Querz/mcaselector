package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonType;
import javafx.scene.image.WritableImage;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.tiles.Selection;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.TileMapSelection;
import net.querz.mcaselector.ui.dialog.AboutDialog;
import net.querz.mcaselector.ui.dialog.CancellableProgressDialog;
import net.querz.mcaselector.ui.dialog.ChangeFieldsConfirmationDialog;
import net.querz.mcaselector.ui.dialog.ChangeNBTDialog;
import net.querz.mcaselector.ui.dialog.DeleteConfirmationDialog;
import net.querz.mcaselector.ui.dialog.ErrorDialog;
import net.querz.mcaselector.ui.dialog.ExportConfirmationDialog;
import net.querz.mcaselector.ui.dialog.FilterChunksDialog;
import net.querz.mcaselector.ui.dialog.GotoDialog;
import net.querz.mcaselector.ui.dialog.ImportConfirmationDialog;
import net.querz.mcaselector.ui.dialog.NBTEditorDialog;
import net.querz.mcaselector.ui.dialog.ProgressDialog;
import net.querz.mcaselector.ui.dialog.SelectWorldDialog;
import net.querz.mcaselector.ui.dialog.SettingsDialog;
import net.querz.mcaselector.ui.dialog.WorldSettingsDialog;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import static net.querz.mcaselector.ui.dialog.ImportConfirmationDialog.ChunkImportConfirmationData;

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
									r.isSelectionOnly() ? new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted()) : null,
									t,
									false
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
											r.isSelectionOnly() ? new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted()) : null,
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
					File dir = createDirectoryChooser(FileHelper.getLastOpenedDirectory("chunk_import_export", null)).showDialog(primaryStage);
					if (dir != null) {
						confRes = new ExportConfirmationDialog(null, primaryStage).showAndWait();
						confRes.ifPresent(confR -> {
							if (confR == ButtonType.OK) {
								FileHelper.setLastOpenedDirectory("chunk_import_export", dir.getAbsolutePath());
								Debug.dump("exporting chunks to " + dir);

								WorldDirectories worldDirectories = FileHelper.createWorldDirectories(dir);
								if (worldDirectories == null) {
									Debug.dump("failed to create world directories");
									new ErrorDialog(primaryStage, "failed to create world directories");
									return;
								}

								new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_FILTERED_CHUNKS, primaryStage)
										.showProgressBar(t -> ChunkFilterExporter.exportFilter(
												r.getFilter(),
												r.isSelectionOnly() ? new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted()) : null,
												worldDirectories,
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
						.showProgressBar(t -> SelectionDeleter.deleteSelection(new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted()), t));
				CacheHelper.clearSelectionCache(tileMap);
				tileMap.clear();
				tileMap.update();
			}
		});
	}

	public static void exportSelectedChunks(TileMap tileMap, Stage primaryStage) {
		File dir = createDirectoryChooser(FileHelper.getLastOpenedDirectory("chunk_import_export", null)).showDialog(primaryStage);
		if (dir != null) {
			Optional<ButtonType> result = new ExportConfirmationDialog(tileMap, primaryStage).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					FileHelper.setLastOpenedDirectory("chunk_import_export", dir.getAbsolutePath());

					WorldDirectories worldDirectories = FileHelper.createWorldDirectories(dir);
					if (worldDirectories == null) {
						Debug.dump("failed to create world directories");
						new ErrorDialog(primaryStage, "failed to create world directories");
						return;
					}

					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_SELECTION, primaryStage)
							.showProgressBar(t -> SelectionExporter.exportSelection(new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted()), worldDirectories, t));
				}
			});
		}
	}

	public static void importChunks(TileMap tileMap, Stage primaryStage) {
		File dir = createDirectoryChooser(FileHelper.getLastOpenedDirectory("chunk_import_export", null)).showDialog(primaryStage);
		DataProperty<ChunkImportConfirmationData> dataProperty = new DataProperty<>();
		if (dir != null) {
			Optional<ButtonType> result = new ImportConfirmationDialog(primaryStage, null, dataProperty::set).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					FileHelper.setLastOpenedDirectory("chunk_import_export", dir.getAbsolutePath());
					DataProperty<Map<Point2i, RegionDirectories>> tempFiles = new DataProperty<>();

					WorldDirectories wd = FileHelper.validateWorldDirectories(dir);
					if (wd == null) {
						// TODO: show error dialog

						return;
					}

					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkImporter.importChunks(
									wd,
									t, false, dataProperty.get().overwrite(),
									null,
									dataProperty.get().selectionOnly() ? new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted()) : null,
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
		result.ifPresent(r -> tileMap.goTo(r.getX(), r.getZ()));
	}

	public static void editSettings(TileMap tileMap, Stage primaryStage) {
		Optional<SettingsDialog.Result> result = new SettingsDialog(primaryStage).showAndWait();
		result.ifPresent(r -> {
			if (Config.getLoadThreads() != r.getReadThreads()
					|| Config.getProcessThreads() != r.getProcessThreads()
					|| Config.getWriteThreads() != r.getWriteThreads()) {
				Config.setLoadThreads(r.getReadThreads());
				Config.setProcessThreads(r.getProcessThreads());
				Config.setWriteThreads(r.getWriteThreads());
				MCAFilePipe.init();
			}
			Config.setMaxLoadedFiles(r.getMaxLoadedFiles());

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
			Config.setMCSavesDir(r.getMcSavesDir() + "");
			Config.setDebug(r.getDebug());
			tileMap.redrawOverlays();
			tileMap.update();
		});
	}

	public static void editWorldSettings(TileMap tileMap, Stage primaryStage) {
		Optional<WorldDirectories> result = new WorldSettingsDialog(primaryStage).showAndWait();
		result.ifPresent(Config::setWorldDirs);
	}

	public static void editNBT(TileMap tileMap, Stage primaryStage) {
		new NBTEditorDialog(tileMap, primaryStage).showAndWait();
	}

	public static void screenshot(TileMap tileMap, Stage primaryStage) {
		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT.makeJavaFXColor());

		WritableImage snapshot = tileMap.snapshot(params, null);

		File file = createFileChooser(FileHelper.getLastOpenedDirectory("snapshot_save", null),
				new FileChooser.ExtensionFilter("*.png Files", "*.png")).showSaveDialog(primaryStage);
		if (file != null) {
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
				FileHelper.setLastOpenedDirectory("snapshot_save", file.getParent());
			} catch (IOException ex) {
				Debug.dumpException("failed to save screenshot", ex);
				new ErrorDialog(primaryStage, ex);
			}
		}
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

			Region from;
			Region to;

			// load from
			try {
				from = Region.loadOrCreateEmptyRegion(FileHelper.createRegionDirectories(fromRegion));
			} catch (IOException ex) {
				Debug.dumpException("failed to load region files", ex);
				t.done(null);
				new ErrorDialog(primaryStage, ex);
				return;
			}

			if (fromRegion.equals(toRegion)) {
				to = from;
			} else {
				try {
					to = Region.loadOrCreateEmptyRegion(FileHelper.createRegionDirectories(fromRegion));
				} catch (IOException ex) {
					Debug.dumpException("failed to load region files", ex);
					t.done(null);
					new ErrorDialog(primaryStage, ex);
					return;
				}
			}

			// get, relocate and set
			Point2i fromOffset = toChunk.sub(fromChunk);
			Point2i toOffset = fromChunk.sub(toChunk);

			ChunkData fromData = from.getChunkDataAt(fromChunk);
			fromData.relocate(fromOffset.chunkToBlock());

			ChunkData toData = to.getChunkDataAt(toChunk);
			toData.relocate(toOffset.chunkToBlock());

			from.setChunkDataAt(toData, fromChunk);
			to.setChunkDataAt(fromData, toChunk);

			t.incrementProgress(FileHelper.createMCAFileName(toRegion));

			try {
				to.saveWithTempFiles();
			} catch (IOException ex) {
				Debug.dumpException("failed to save region files", ex);
				t.done(null);
				new ErrorDialog(primaryStage, ex);
				return;
			}
			if (to != from) {
				try {
					from.saveWithTempFiles();
				} catch (IOException ex) {
					Debug.dumpException("failed to save region files", ex);
					t.done(null);
					new ErrorDialog(primaryStage, ex);
					return;
				}
			}

			t.done(Translation.DIALOG_PROGRESS_DONE.toString());

			Platform.runLater(() -> CacheHelper.clearSelectionCache(tileMap));
		});
	}

	public static void copySelectedChunks(TileMap tileMap) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Selection selection = new Selection(tileMap.getMarkedChunks(), tileMap.isSelectionInverted(), Config.getWorldDirs());
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
					DataProperty<Map<Point2i, RegionDirectories>> tempFiles = new DataProperty<>();
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkImporter.importChunks(
									tileMap.getPastedWorld(), t, false, dataProperty.get().overwrite(),
									new SelectionData(tileMap.getPastedChunks(), tileMap.getPastedChunksInverted()),
									dataProperty.get().selectionOnly() ? new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted()) : null,
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

					tileMap.setPastedChunks(selection.getSelectionData(), selection.isInverted(), selection.getMin(), selection.getMax(), selection.getWorld());
					tileMap.update();

				} catch (UnsupportedFlavorException | IOException ex) {
					Debug.dumpException("failed to paste chunks", ex);
				}
			}
		}
	}

	private static void deleteTempFiles(Map<Point2i, RegionDirectories> tempFiles) {
		if (tempFiles != null) {
			for (RegionDirectories tempFile : tempFiles.values()) {
				if (!tempFile.getRegion().delete()) {
					Debug.errorf("failed to delete temp file %s", tempFile.getRegion());
				}
				if (!tempFile.getPoi().delete()) {
					Debug.errorf("failed to delete temp file %s", tempFile.getPoi());
				}
				if (!tempFile.getEntities().delete()) {
					Debug.errorf("failed to delete temp file %s", tempFile.getEntities());
				}
			}
		}
	}

	public static void openRegion(TileMap tileMap, Stage primaryStage, OptionBar optionBar) {
		String lastOpenDirectory = FileHelper.getLastOpenedDirectory("open_world", Config.getMCSavesDir());
		File file = createDirectoryChooser(lastOpenDirectory).showDialog(primaryStage);
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
				tileMap.getWindow().setTitleSuffix(file.toString());
			} else {
				new ErrorDialog(primaryStage, String.format("no mca files found in %s", file));
			}
		} else if (file != null) {
			new ErrorDialog(primaryStage, String.format("%s is not a directory", file));
		}
	}

	public static void openWorld(TileMap tileMap, Stage primaryStage, OptionBar optionBar) {
		String lastOpenDirectory = FileHelper.getLastOpenedDirectory("open_world", Config.getMCSavesDir());
		File file = createDirectoryChooser(lastOpenDirectory).showDialog(primaryStage);
		if (file != null && file.isDirectory()) {
			List<File> dimensions = detectDimensionDirectories(file);
			if (dimensions.size() == 0) {
				new ErrorDialog(primaryStage, String.format("no dimensions found in %s", file.getAbsolutePath()));
				Debug.dumpf("no dimensions found in %s", file.getAbsolutePath());
				return;
			}

			// if there is only one dimension, open it instantly
			if (dimensions.size() == 1) {
				setWorld(detectWorldDirectories(dimensions.get(0)), tileMap, optionBar);
				return;
			}

			// show world selection dialog
			Optional<File> result = new SelectWorldDialog(dimensions, tileMap, primaryStage).showAndWait();
			result.ifPresent(dim -> {
				setWorld(detectWorldDirectories(dim), tileMap, optionBar);
			});
		} else if (file != null) {
			new ErrorDialog(primaryStage, String.format("%s is not a directory", file));
		}
	}

	private static void setWorld(WorldDirectories worldDirectories, TileMap tileMap, OptionBar optionBar) {
		Config.setWorldDirs(worldDirectories);
		CacheHelper.validateCacheVersion(tileMap);
		tileMap.clear();
		tileMap.update();
		tileMap.disable(false);
		optionBar.setWorldDependentMenuItemsEnabled(true, tileMap);
		tileMap.getWindow().setTitleSuffix(worldDirectories.getRegion().getParent());
	}

	public static void importSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(FileHelper.getLastOpenedDirectory("selection_import_export", null),
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showOpenDialog(primaryStage);
		if (file != null) {
			SelectionData selection = SelectionHelper.importSelection(file);
			FileHelper.setLastOpenedDirectory("selection_import_export", file.getParent());
			tileMap.setMarkedChunks(selection.getSelection());
			tileMap.setSelectionInverted(selection.isInverted());
			tileMap.update();
		}
	}

	public static void exportSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(FileHelper.getLastOpenedDirectory("selection_import_export", null),
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showSaveDialog(primaryStage);
		if (file != null) {
			SelectionHelper.exportSelection(new SelectionData(tileMap.getMarkedChunks(), tileMap.isSelectionInverted()), file);
			FileHelper.setLastOpenedDirectory("selection_import_export", file.getParent());
			tileMap.update();
		}
	}

	public static DirectoryChooser createDirectoryChooser(String initialDirectory) {
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

	public static WorldDirectories detectWorldDirectories(File dir) {
		File region = new File(dir, "region");
		File poi = new File(dir, "poi");
		File entities = new File(dir, "entities");

		WorldDirectories worldDirectories = new WorldDirectories();

		if (region.exists() && hasMCAFiles(region)) {
			worldDirectories.setRegion(region);
		}
		if (poi.exists()) {
			worldDirectories.setPoi(poi);
		}
		if (entities.exists()) {
			worldDirectories.setEntities(entities);
		}

		return worldDirectories;
	}

	// takes a directory and detects all world directories
	public static List<File> detectDimensionDirectories(File dir) {
		List<File> result = new ArrayList<>();

		// detect overworld
		if (isValidDimension(dir)) {
			result.add(dir);
		}

		// detect nether folder and end folder first to have them at the beginning of the list
		File nether = new File(dir, "DIM-1");
		if (isValidDimension(nether)) {
			result.add(nether);
		}
		File end = new File(dir, "DIM1");
		if (isValidDimension(end)) {
			result.add(end);
		}

		// detect custom dimensions
		File[] customDimensions = dir.listFiles((d, name) -> !name.equals("DIM-1") && !name.equals("DIM1") && name.matches("^DIM-?\\d+$"));
		if (customDimensions != null && customDimensions.length > 0) {
			for (File customDimension : customDimensions) {
				if (isValidDimension(customDimension)) {
					result.add(customDimension);
				}
			}
		}

		return result;
	}

	private static boolean isValidDimension(File dir) {
		File region = new File(dir, "region");
		return region.exists() && hasMCAFiles(region);
	}

	private static boolean hasMCAFiles(File dir) {
		File[] files = dir.listFiles((d, name) -> name.matches(FileHelper.MCA_FILE_PATTERN));
		return files != null && files.length > 0;
	}
}
