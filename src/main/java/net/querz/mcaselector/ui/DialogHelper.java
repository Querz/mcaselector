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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DialogHelper {


	public static void showAboutDialog(Stage primaryStage) {
		new AboutDialog(primaryStage).showAndWait();
	}

	public static void changeFields(TileMap tileMap, Stage primaryStage) {
		Optional<ChangeNBTDialog.Result> result = new ChangeNBTDialog(primaryStage).showAndWait();
		result.ifPresent(r -> {
			Optional<ButtonType> confRes = new ChangeFieldsConfirmationDialog(null, primaryStage).showAndWait();
			confRes.ifPresent(confR -> {
				if (confR == ButtonType.OK) {
					new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_CHANGING_NBT_DATA, primaryStage)
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
							new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_DELETING_FILTERED_CHUNKS, primaryStage)
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
					File dir = createDirectoryChooser(null).showDialog(primaryStage);
					if (dir != null) {
						confRes = new ExportConfirmationDialog(null, primaryStage).showAndWait();
						confRes.ifPresent(confR -> {
							if (confR == ButtonType.OK) {
								Debug.dump("exporting chunks to " + dir);
								new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_FILTERED_CHUNKS, primaryStage)
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
					new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_SELECTING_FILTERED_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkFilterSelector.selectFilter(r.getFilter(), selection -> Platform.runLater(() -> {
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
				new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_DELETING_SELECTION, primaryStage)
						.showProgressBar(t -> SelectionDeleter.deleteSelection(tileMap.getMarkedChunks(), t));
				CacheHelper.clearSelectionCache(tileMap);
			}
		});
	}

	public static void exportSelectedChunks(TileMap tileMap, Stage primaryStage) {
		File dir = createDirectoryChooser(null).showDialog(primaryStage);
		if (dir != null) {
			Optional<ButtonType> result = new ExportConfirmationDialog(tileMap, primaryStage).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_SELECTION, primaryStage)
							.showProgressBar(t -> SelectionExporter.exportSelection(tileMap.getMarkedChunks(), dir, t));
				}
			});
		}
	}

	public static void importChunks(TileMap tileMap, Stage primaryStage) {
		File dir = createDirectoryChooser(null).showDialog(primaryStage);
		DataProperty<ImportConfirmationDialog.ChunkImportConfirmationData> dataProperty = new DataProperty<>();
		if (dir != null) {
			Optional<ButtonType> result = new ImportConfirmationDialog(primaryStage, dataProperty::set).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
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

	public static void openWorld(TileMap tileMap, Stage primaryStage, OptionBar optionBar) {
		String savesDir = FileHelper.getMCSavesDir();
		File file = createDirectoryChooser(savesDir).showDialog(primaryStage);
		if (file != null && file.isDirectory()) {
			File[] files = file.listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.mca$"));
			if (files != null && files.length > 0) {
				Debug.dump("setting world dir to " + file.getAbsolutePath());
				Config.setWorldDir(file);
				tileMap.clear();
				tileMap.update();
				optionBar.setWorldDependentMenuItemsEnabled(true);
			}
		}
	}

	public static void importSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(null,
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showOpenDialog(primaryStage);
		if (file != null) {
			Map<Point2i, Set<Point2i>> chunks = SelectionUtil.importSelection(file);
			tileMap.setMarkedChunks(chunks);
			tileMap.update();
		}
	}

	public static void exportSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(null,
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showSaveDialog(primaryStage);
		if (file != null) {
			SelectionUtil.exportSelection(tileMap.getMarkedChunks(), file);
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
