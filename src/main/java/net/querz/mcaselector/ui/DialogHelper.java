package net.querz.mcaselector.ui;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonType;
import javafx.scene.image.WritableImage;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.io.job.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.ClipboardSelection;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.selection.SelectionData;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.dialog.*;
import net.querz.mcaselector.validation.BeforeAfterCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static net.querz.mcaselector.ui.dialog.ImportConfirmationDialog.ChunkImportConfirmationData;

public class DialogHelper {

	private static final Logger LOGGER = LogManager.getLogger(DialogHelper.class);

	public static void showAboutDialog(Stage primaryStage) {
		new AboutDialog(primaryStage).showAndWait();
	}

	public static void changeFields(TileMap tileMap, Stage primaryStage) {
		Optional<ChangeNBTDialog.Result> result = new ChangeNBTDialog(tileMap, primaryStage).showAndWait();
		result.ifPresent(r -> {
			Optional<ButtonType> confRes = new ChangeFieldsConfirmationDialog(null, primaryStage).showAndWait();
			confRes.ifPresent(confR -> {
				if (confR == ButtonType.OK) {
					if (runBefore(r, primaryStage)) {
						return;
					}
					CancellableProgressDialog c = new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_CHANGING_NBT_DATA, primaryStage);
					c.showProgressBar(t -> FieldChanger.changeNBTFields(
							r.fields(),
							r.force(),
							r.selectionOnly() ? tileMap.getSelection() : null,
							t,
							false
					));
					if (r.requiresClearCache()) {
						if (r.selectionOnly()) {
							CacheHelper.clearSelectionCache(tileMap);
						} else {
							CacheHelper.clearAllCache(tileMap);
						}
					}
					if (c.cancelled()) {
						return;
					}
					runAfter(r, primaryStage);
				}
			});
		});
	}

	public static void filterChunks(TileMap tileMap, Stage primaryStage) {
		Optional<FilterChunksDialog.Result> result = new FilterChunksDialog(primaryStage).showAndWait();
		result.ifPresent(r -> {
			LOGGER.debug("chunk filter query: {}", r.filter());
			if (r.filter().isEmpty()) {
				LOGGER.debug("filter is empty, won't delete everything");
				return;
			}

			switch (r.type()) {
				case DELETE -> {
					Optional<ButtonType> confRes = new DeleteConfirmationDialog(null, primaryStage).showAndWait();
					confRes.ifPresent(confR -> {
						if (confR == ButtonType.OK) {
							if (runBefore(r, primaryStage)) {
								return;
							}
							CancellableProgressDialog c = new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_DELETING_FILTERED_CHUNKS, primaryStage);
							c.showProgressBar(t -> ChunkFilterDeleter.deleteFilter(
								r.filter(),
								r.selectionOnly() ? tileMap.getSelection() : null,
								t,
								false
							));
							r.filter().resetTempData();
							if (r.selectionOnly()) {
								CacheHelper.clearSelectionCache(tileMap);
								tileMap.clear();
								tileMap.clearSelection();
							} else {
								CacheHelper.clearAllCache(tileMap);
							}
							if (c.cancelled()) {
								return;
							}
							runAfter(r, primaryStage);
						}
					});
				}
				case EXPORT -> {
					File dir = createDirectoryChooser(FileHelper.getLastOpenedDirectory("chunk_import_export", null)).showDialog(primaryStage);
					if (dir != null) {
						Optional<ButtonType> confRes = new ExportConfirmationDialog(null, primaryStage).showAndWait();
						confRes.ifPresent(confR -> {
							if (confR == ButtonType.OK) {
								FileHelper.setLastOpenedDirectory("chunk_import_export", dir.getAbsolutePath());
								LOGGER.debug("exporting chunks to {}", dir);

								WorldDirectories worldDirectories = FileHelper.createWorldDirectories(dir);
								if (worldDirectories == null) {
									LOGGER.warn("failed to create world directories");
									new ErrorDialog(primaryStage, "failed to create world directories");
									return;
								}

								if (runBefore(r, primaryStage)) {
									return;
								}
								CancellableProgressDialog c = new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_FILTERED_CHUNKS, primaryStage);
								c.showProgressBar(t -> ChunkFilterExporter.exportFilter(
									r.filter(),
									r.selectionOnly() ? tileMap.getSelection() : null,
									worldDirectories,
									t,
									false
								));
								r.filter().resetTempData();
								if (c.cancelled()) {
									return;
								}
								runAfter(r, primaryStage);
							}
						});
					} else {
						LOGGER.debug("cancelled exporting chunks, no valid destination directory");
					}
				}
				case SELECT -> {
					Selection selection = tileMap.getSelection();
					if (r.overwriteSelection()) {
						tileMap.clearSelection();
					}
					if (runBefore(r, primaryStage)) {
						break;
					}
					CancellableProgressDialog c = new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_SELECTING_FILTERED_CHUNKS, primaryStage);
					c.showProgressBar(t -> ChunkFilterSelector.selectFilter(
						r.filter(),
						r.selectionOnly() ? (selection.isEmpty() ? null : selection) : null,
						r.radius(),
						s -> Platform.runLater(() -> {
							tileMap.addSelection(s);
							tileMap.draw();
						}), t, false));
					r.filter().resetTempData();
					if (c.cancelled()) {
						break;
					}
					runAfter(r, primaryStage);
				}
				default -> LOGGER.debug("i have no idea how you got no selection there...");
			}
		});
		tileMap.draw();
	}

	private static boolean runBefore(BeforeAfterCallback result, Stage primaryStage) {
		if (result.valid()) {
			CancellableProgressDialog c = new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_RUNNING_BEFORE, primaryStage);
			c.showProgressBar(t -> {
				t.setIndeterminate("running before()...");
				result.before();
				t.done("done");
			});
			return c.cancelled();
		}
		return false;
	}

	private static boolean runAfter(BeforeAfterCallback result, Stage primaryStage) {
		if (result.valid()) {
			CancellableProgressDialog c = new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_RUNNING_AFTER, primaryStage);
			c.showProgressBar(t -> {
				t.setIndeterminate("running after()...");
				result.after();
				t.done("done");
			});
			return c.cancelled();
		}
		return false;
	}

	public static void quit(TileMap tileMap, Stage primaryStage) {
		if (tileMap.hasUnsavedSelection()) {
			Optional<ButtonType> result = new ConfirmationDialog(primaryStage, Translation.DIALOG_UNSAVED_SELECTION_TITLE, Translation.DIALOG_UNSAVED_SELECTION_HEADER, "unsaved-changes").showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					System.exit(0);
				}
			});
		} else {
			System.exit(0);
		}
	}

	public static void editOverlays(TileMap tileMap, Stage primaryStage) {
		new OverlayEditorDialog(primaryStage, tileMap, tileMap.getOverlays()).show();
	}

	public static void deleteSelection(TileMap tileMap, Stage primaryStage) {
		Optional<ButtonType> result = new DeleteConfirmationDialog(tileMap, primaryStage).showAndWait();
		result.ifPresent(r -> {
			if (r == ButtonType.OK) {
				new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_DELETING_SELECTION, primaryStage)
						.showProgressBar(t -> SelectionDeleter.deleteSelection(tileMap.getSelection(), t));
				CacheHelper.clearSelectionCache(tileMap);
				tileMap.clear();
				tileMap.clearSelection();
				tileMap.draw();
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
						LOGGER.warn("failed to create world directories");
						new ErrorDialog(primaryStage, "failed to create world directories");
						return;
					}

					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_SELECTION, primaryStage)
							.showProgressBar(t -> SelectionExporter.exportSelection(tileMap.getSelection(), worldDirectories, t));
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
						new ErrorDialog(primaryStage, "invalid world directory, missing 'region' directory");
						return;
					}

					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkImporter.importChunks(
									wd,
									t, false, dataProperty.get().overwrite(),
									null,
									dataProperty.get().selectionOnly() ? tileMap.getSelection() : null,
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

	public static void editSettings(TileMap tileMap, Stage primaryStage, boolean renderSettings) {
		// ignore request to display render settings directly if there is no world
		if (tileMap.getDisabled() && renderSettings) {
			return;
		}

		Optional<SettingsDialog.Result> result = new SettingsDialog(primaryStage, renderSettings).showAndWait();
		result.ifPresent(r -> {
			if (ConfigProvider.GLOBAL.getProcessThreads() != r.processThreads
					|| ConfigProvider.GLOBAL.getWriteThreads() != r.writeThreads) {
				ConfigProvider.GLOBAL.setProcessThreads(r.processThreads);
				ConfigProvider.GLOBAL.setWriteThreads(r.writeThreads);
				JobHandler.init();
			}
			ConfigProvider.GLOBAL.setMaxLoadedFiles(r.maxLoadedFiles);

			if (!ConfigProvider.GLOBAL.getLocale().equals(r.locale)) {
				ConfigProvider.GLOBAL.setLocale(r.locale);
				Locale.setDefault(ConfigProvider.GLOBAL.getLocale());
			}
			ConfigProvider.GLOBAL.setRegionSelectionColor(new Color(r.regionColor));
			ConfigProvider.GLOBAL.setChunkSelectionColor(new Color(r.chunkColor));
			ConfigProvider.GLOBAL.setPasteChunksColor(new Color(r.pasteColor));
			tileMap.redrawOverlays();
			ConfigProvider.GLOBAL.setMcSavesDir(r.mcSavesDir + "");
			ConfigProvider.GLOBAL.setDebug(r.debug);

			if (!tileMap.getDisabled()) {
				ConfigProvider.WORLD.setShowNonexistentRegions(r.showNonexistentRegions);
				tileMap.setShowNonexistentRegions(r.showNonexistentRegions);
				ConfigProvider.WORLD.setSmoothRendering(r.smoothRendering);
				tileMap.setSmoothRendering(r.smoothRendering);
				ConfigProvider.WORLD.setSmoothOverlays(r.smoothOverlays);
				ConfigProvider.WORLD.setTileMapBackground(r.tileMapBackground.name());
				tileMap.getWindow().getTileMapBox().setBackground(r.tileMapBackground.getBackground());

				if (r.height != ConfigProvider.WORLD.getRenderHeight() || r.layerOnly != ConfigProvider.WORLD.getRenderLayerOnly()
					|| r.shade != ConfigProvider.WORLD.getShade() || r.shadeWater != ConfigProvider.WORLD.getShadeWater() || r.caves != ConfigProvider.WORLD.getRenderCaves()) {
					ConfigProvider.WORLD.setRenderHeight(r.height);
					ConfigProvider.WORLD.setRenderLayerOnly(r.layerOnly);
					ConfigProvider.WORLD.setRenderCaves(r.caves);
					tileMap.getWindow().getOptionBar().setRenderHeight(r.height);
					ConfigProvider.WORLD.setShade(r.shade);
					ConfigProvider.WORLD.setShadeWater(r.shadeWater);
					// only clear the cache if the actual image rendering changed
					CacheHelper.clearAllCache(tileMap);
				}

				WorldDirectories worldDirectories = ConfigProvider.WORLD.getWorldDirs();
				worldDirectories.setPoi(r.poi);
				worldDirectories.setEntities(r.entities);

				ConfigProvider.WORLD.save();
			}

			ConfigProvider.GLOBAL.save();
			ConfigProvider.OVERLAY.save();

			tileMap.draw();
		});
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
				LOGGER.warn("failed to save screenshot", ex);
				new ErrorDialog(primaryStage, ex);
			}
		}
	}

	public static void generateImageFromSelection(TileMap tileMap, Stage primaryStage) {
		net.querz.mcaselector.selection.SelectionData data = new net.querz.mcaselector.selection.SelectionData(tileMap.getSelection(), null);

		if (data.getWidth() * 16 * data.getHeight() * 16 > Integer.MAX_VALUE) {
			String error = String.format("dimensions are too large to generate an image: %dx%d", data.getWidth() * 16, data.getHeight() * 16);
			LOGGER.warn(error);
			new ErrorDialog(primaryStage, error);
			return;
		}

		File file = createFileChooser(FileHelper.getLastOpenedDirectory("snapshot_save", null),
				new FileChooser.ExtensionFilter("*.png Files", "*.png")).showSaveDialog(primaryStage);
		if (file == null) {
			return;
		}

		Optional<ButtonType> result = new ImageExportConfirmationDialog(tileMap, data, primaryStage).showAndWait();
		result.ifPresent(b -> {
			if (b == ButtonType.OK) {
				DataProperty<int[]> pixels = new DataProperty<>();
				CancellableProgressDialog cpd = new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_CREATING_IMAGE, primaryStage);
				cpd.showProgressBar(t -> pixels.set(SelectionImageExporter.exportSelectionImage(data, tileMap.getOverlayPool(), t)));
				if (!cpd.cancelled() && pixels.get() != null) {
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_SAVING_IMAGE, primaryStage)
					.showProgressBar(t -> {
						try {
							ImageHelper.saveImageData(pixels.get(), (int) data.getWidth() * 16, (int) data.getHeight() * 16, file, t);
							FileHelper.setLastOpenedDirectory("snapshot_save", file.getParent());
						} catch (IOException ex) {
							LOGGER.warn("failed to save image", ex);
							new ErrorDialog(primaryStage, ex);
						}
					});
				}
			}
		});
	}

	public static void swapChunks(TileMap tileMap, Stage primaryStage) {
		new ProgressDialog(Translation.MENU_TOOLS_SWAP_CHUNKS, primaryStage).showProgressBar(t -> {
			t.setMax(4);
			Selection markedChunks = tileMap.getSelection();
			LongArrayList chunks = new LongArrayList(2);
			for (Long2ObjectMap.Entry<ChunkSet> entry : markedChunks) {
				for (int markedChunk : entry.getValue()) {
					chunks.add(new Point2i(entry.getLongKey()).regionToChunk().add(new Point2i(markedChunk)).asLong());
				}
			}
			if (chunks.size() != 2) {
				throw new IllegalStateException("need 2 chunks to swap");
			}

			Point2i fromChunk = new Point2i(chunks.getLong(0));
			Point2i toChunk = new Point2i(chunks.getLong(1));
			Point2i fromRegion = fromChunk.chunkToRegion();
			Point2i toRegion = toChunk.chunkToRegion();

			LOGGER.debug("swapping chunk {}:{} with {}:{}", fromChunk, fromRegion, toChunk, toRegion);

			t.incrementProgress(FileHelper.createMCAFileName(fromRegion));

			Region from;
			Region to;

			// load from
			try {
				from = Region.loadOrCreateEmptyRegion(FileHelper.createRegionDirectories(fromRegion));
			} catch (IOException ex) {
				LOGGER.warn("failed to load region files", ex);
				t.done(null);
				new ErrorDialog(primaryStage, ex);
				return;
			}

			if (fromRegion.equals(toRegion)) {
				to = from;
			} else {
				try {
					to = Region.loadOrCreateEmptyRegion(FileHelper.createRegionDirectories(toRegion));
				} catch (IOException ex) {
					LOGGER.warn("failed to load region files", ex);
					t.done(null);
					new ErrorDialog(primaryStage, ex);
					return;
				}
			}

			// get, relocate and set
			Point2i fromOffset = toChunk.sub(fromChunk);
			Point2i toOffset = fromChunk.sub(toChunk);

			ChunkData fromData = from.getChunkDataAt(fromChunk, true);
			fromData.relocate(fromOffset.chunkToBlock().toPoint3i());

			ChunkData toData = to.getChunkDataAt(toChunk, true);
			toData.relocate(toOffset.chunkToBlock().toPoint3i());

			from.setChunkDataAt(toData, fromChunk);
			to.setChunkDataAt(fromData, toChunk);

			t.incrementProgress(FileHelper.createMCAFileName(toRegion));

			try {
				to.saveWithTempFiles();
			} catch (IOException ex) {
				LOGGER.warn("failed to save region files", ex);
				t.done(null);
				new ErrorDialog(primaryStage, ex);
				return;
			}
			if (to != from) {
				try {
					from.saveWithTempFiles();
				} catch (IOException ex) {
					LOGGER.warn("failed to save region files", ex);
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
		SelectionData data = new SelectionData(tileMap.getSelection(), ConfigProvider.WORLD.getWorldDirs());
		ClipboardSelection clipboardSelection = new ClipboardSelection(data);
		clipboard.setContents(clipboardSelection, tileMap);
	}

	public static void pasteSelectedChunks(TileMap tileMap, Stage primaryStage) {
		if (tileMap.isInPastingMode()) {
			DataProperty<ImportConfirmationDialog.ChunkImportConfirmationData> dataProperty = new DataProperty<>();
			ChunkImportConfirmationData preFill = new ChunkImportConfirmationData(tileMap.getPastedChunksOffset(), 0, true, false, null);
			Optional<ButtonType> result = new ImportConfirmationDialog(primaryStage, preFill, dataProperty::set).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					DataProperty<Map<Point2i, RegionDirectories>> tempFiles = new DataProperty<>();
					new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkImporter.importChunks(
									tileMap.getPastedWorld(), t, false, dataProperty.get().overwrite(),
									tileMap.getPastedChunks(),
									dataProperty.get().selectionOnly() ? tileMap.getSelection() : null,
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

			if (flavors.length == 1 && flavors[0].equals(ClipboardSelection.SELECTION_DATA_FLAVOR)) {
				try {
					Object data = content.getTransferData(flavors[0]);

					net.querz.mcaselector.selection.SelectionData selectionData = (net.querz.mcaselector.selection.SelectionData) data;

					tileMap.setPastedChunks(selectionData);
					tileMap.draw();

				} catch (UnsupportedFlavorException | IOException ex) {
					LOGGER.warn("failed to paste chunks", ex);
				}
			}
		}
	}

	private static void deleteTempFiles(Map<Point2i, RegionDirectories> tempFiles) {
		if (tempFiles != null) {
			for (RegionDirectories tempFile : tempFiles.values()) {
				if (!tempFile.getRegion().delete()) {
					LOGGER.warn("failed to delete temp file {}", tempFile.getRegion());
				}
				if (!tempFile.getPoi().delete()) {
					LOGGER.warn("failed to delete temp file {}", tempFile.getPoi());
				}
				if (!tempFile.getEntities().delete()) {
					LOGGER.warn("failed to delete temp file {}", tempFile.getEntities());
				}
			}
		}
	}

	public static void openWorld(TileMap tileMap, Stage primaryStage) {
		if (tileMap.hasUnsavedSelection()) {
			Optional<ButtonType> result = new ConfirmationDialog(primaryStage, Translation.DIALOG_UNSAVED_SELECTION_TITLE, Translation.DIALOG_UNSAVED_SELECTION_HEADER, "unsaved-changes").showAndWait();
			if (result.isPresent() && result.get() == ButtonType.CANCEL) {
				return;
			}
		}

		String lastOpenDirectory = FileHelper.getLastOpenedDirectory("open_world", ConfigProvider.GLOBAL.getMcSavesDir());
		File file = createDirectoryChooser(lastOpenDirectory).showDialog(primaryStage);
		if (file != null && file.isDirectory()) {
			List<File> dimensions = FileHelper.detectDimensionDirectories(file);
			if (dimensions.isEmpty()) {
				new ErrorDialog(primaryStage, String.format("no dimensions found in %s", file.getAbsolutePath()));
				LOGGER.warn("no dimensions found in {}", file.getAbsolutePath());
				return;
			}

			FileHelper.setLastOpenedDirectory("open_world", file.getAbsolutePath());

			// if there is only one dimension, open it instantly
			if (dimensions.size() == 1) {
				setWorld(FileHelper.detectWorldDirectories(dimensions.get(0)), dimensions, tileMap, primaryStage);
				return;
			}
			// show world selection dialog
			Optional<File> result = new SelectWorldDialog(dimensions, primaryStage).showAndWait();
			result.ifPresent(dim -> setWorld(FileHelper.detectWorldDirectories(dim), dimensions, tileMap, primaryStage));
		} else if (file != null) {
			new ErrorDialog(primaryStage, String.format("%s is not a directory", file));
		}
	}

	public static void setWorld(WorldDirectories worldDirectories, List<File> dimensionDirectories, TileMap tileMap, Stage primaryStage) {
		ConfigProvider.GLOBAL.addRecentWorld(worldDirectories.getRegion().getParentFile(), dimensionDirectories);

		new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_LOADING_WORLD, primaryStage).showProgressBar((task) -> {
			ConfigProvider.loadWorldConfig(worldDirectories, dimensionDirectories);
			CacheHelper.validateCacheVersion(tileMap);
			RegionImageGenerator.invalidateCachedMCAFiles();
			tileMap.getWindow().getOptionBar().setRenderHeight(ConfigProvider.WORLD.getRenderHeight());
			tileMap.clear(task);
			tileMap.clearSelection();
			tileMap.draw();
			tileMap.disable(false);
			tileMap.getWindow().getOptionBar().setWorldDependentMenuItemsEnabled(true, tileMap, primaryStage);
			tileMap.getOverlayPool().switchTo(new File(ConfigProvider.WORLD.getCacheDir(), "cache.db").toString(), tileMap.getOverlays());
			task.done(Translation.DIALOG_PROGRESS_DONE.toString());
			Platform.runLater(() -> tileMap.getWindow().setTitleSuffix(worldDirectories.getRegion().getParent()));
		});
	}

	public static void importSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(FileHelper.getLastOpenedDirectory("selection_import_export", null),
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showOpenDialog(primaryStage);
		if (file != null) {
			ImportSelectionDialog.Result result = ImportSelectionDialog.Result.OVERWRITE;
			// skip dialog if we don't have a selection yet
			if (tileMap.getSelectedChunks() > 0) {
				Optional<ImportSelectionDialog.Result> optional = new ImportSelectionDialog(primaryStage).showAndWait();
				if (optional.isEmpty()) {
					return;
				}
				result = optional.get();
			}

			Selection selection;
			try {
				selection = Selection.readFromFile(file);
			} catch (IOException ex) {
				LOGGER.warn("failed to read selection from file", ex);
				new ErrorDialog(primaryStage, ex.getMessage());
				return;
			}
			FileHelper.setLastOpenedDirectory("selection_import_export", file.getParent());
			switch (result) {
				case OVERWRITE -> tileMap.setSelection(selection);
				case MERGE -> tileMap.addSelection(selection);
			}
			tileMap.draw();
		}
	}

	public static void exportSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(FileHelper.getLastOpenedDirectory("selection_import_export", null),
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showSaveDialog(primaryStage);
		if (file != null) {
			try {
				tileMap.getSelection().saveToFile(file);
			} catch (IOException ex) {
				LOGGER.warn("failed to save selection to file", ex);
				new ErrorDialog(primaryStage, ex);
				return;
			}
			FileHelper.setLastOpenedDirectory("selection_import_export", file.getParent());
			tileMap.setSelectionSaved();
			tileMap.draw();
		}
	}

	public static void sumSelection(TileMap tileMap, Stage primaryStage) {
		if(tileMap.getOverlay() == null) { // here can be checked if the overlay does not make sense for summing (ex. Timestamp, LastUpdate, etc.)
			String error = "a right overlay must be active in order to sum the selection";
			LOGGER.warn(error);
			new ErrorDialog(primaryStage, error);
			return;
		}
		DataProperty<AtomicLong> sum = new DataProperty<>();
		CancellableProgressDialog cpd = new CancellableProgressDialog(Translation.DIALOG_PROGRESS_TITLE_SUMMING, primaryStage);
		cpd.showProgressBar(t -> sum.set(SelectionSummer.sumSelection(tileMap.getSelectedChunks(), tileMap.getSelection(), tileMap.getOverlay(), t)));
		if (!cpd.cancelled()) {
			String s = tileMap.getOverlay().getShortMultiValues();
			String title = tileMap.getOverlay().getType() + (s == null ? "" : "(" + s + ")");
			new NumberDialog(primaryStage, sum.get().get(), title).showAndWait();
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
}
