package net.querz.mcaselector.ui.component;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.github.VersionChecker;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.selection.ClipboardSelection;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.io.CacheHelper;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.DialogHelper;
import net.querz.mcaselector.ui.UIFactory;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;

public class OptionBar extends BorderPane {
	/*
	* File				View				Selection					Tools				About
	* - Open World		- Chunk Grid		- Clear         			- Import chunks
	* - Open Region		- Region Grid		- Invert        			- Filter chunks
	* - Settings		- Goto				- Copy chunks				- Change fields
	* - Render Settings	- Reset Zoom    	- Paste chunks				- Edit chunk
	* - Quit			- Save Screenshot	- Export selected chunks	- Swap chunks
	*					- Clear cache   	- Delete selected chunks	- Edit overlays
	*					- Clear all cache	- Import selection			- Next overlay
	*										- Export selection          - Next overlay type
	*                                       - Export as image			- Sum selection
	* 										- Clear cache
	* */

	private final MenuBar menuBar = new MenuBar();

	private final Menu file = UIFactory.menu(Translation.MENU_FILE);
	private final Menu view = UIFactory.menu(Translation.MENU_VIEW);
	private final Menu selection = UIFactory.menu(Translation.MENU_SELECTION);
	private final Menu tools = UIFactory.menu(Translation.MENU_TOOLS);
	private final Label about = UIFactory.label(Translation.MENU_ABOUT);

	private final HeightSlider hSlider = new HeightSlider(319, true);
	private final MenuItem openWorld = UIFactory.menuItem(Translation.MENU_FILE_OPEN_WORLD);
	private final Menu openDimension = UIFactory.menu(Translation.MENU_FILE_OPEN_DIMENSION);
	private final Menu openRecent = UIFactory.menu(Translation.MENU_FILE_OPEN_RECENT_WORLD);
	private final MenuItem settings = UIFactory.menuItem(Translation.MENU_FILE_SETTINGS);
	private final MenuItem renderSettings = UIFactory.menuItem(Translation.MENU_FILE_RENDER_SETTINGS);
	private final MenuItem quit = UIFactory.menuItem(Translation.MENU_FILE_QUIT);
	private final MenuItem reload = UIFactory.menuItem(Translation.MENU_VIEW_RELOAD);
	private final CheckMenuItem chunkGrid = UIFactory.checkMenuItem(Translation.MENU_VIEW_CHUNK_GRID, true);
	private final CheckMenuItem regionGrid = UIFactory.checkMenuItem(Translation.MENU_VIEW_REGION_GRID, true);
	private final CheckMenuItem coordinates = UIFactory.checkMenuItem(Translation.MENU_VIEW_COORDINATES, false);
	private final MenuItem goTo = UIFactory.menuItem(Translation.MENU_VIEW_GOTO);
	private final MenuItem resetZoom = UIFactory.menuItem(Translation.MENU_VIEW_RESET_ZOOM);
	private final MenuItem saveScreenshot = UIFactory.menuItem(Translation.MENU_VIEW_SAVE_SCREENSHOT);
	private final MenuItem clearViewCache = UIFactory.menuItem(Translation.MENU_VIEW_CLEAR_CACHE);
	private final MenuItem clearAllCache = UIFactory.menuItem(Translation.MENU_VIEW_CLEAR_ALL_CACHE);
	private final MenuItem clear = UIFactory.menuItem(Translation.MENU_SELECTION_CLEAR);
	private final MenuItem invert = UIFactory.menuItem(Translation.MENU_SELECTION_INVERT);
	private final MenuItem invertRegions = UIFactory.menuItem(Translation.MENU_SELECTION_INVERT_REGIONS);
	private final MenuItem copy = UIFactory.menuItem(Translation.MENU_SELECTION_COPY_CHUNKS);
	private final MenuItem paste = UIFactory.menuItem(Translation.MENU_SELECTION_PASTE_CHUNKS);
	private final MenuItem exportChunks = UIFactory.menuItem(Translation.MENU_SELECTION_EXPORT_CHUNKS);
	private final MenuItem importChunks = UIFactory.menuItem(Translation.MENU_TOOLS_IMPORT_CHUNKS);
	private final MenuItem delete = UIFactory.menuItem(Translation.MENU_SELECTION_DELETE_CHUNKS);
	private final MenuItem importSelection = UIFactory.menuItem(Translation.MENU_SELECTION_IMPORT_SELECTION);
	private final MenuItem exportSelection = UIFactory.menuItem(Translation.MENU_SELECTION_EXPORT_SELECTION);
	private final MenuItem exportImage = UIFactory.menuItem(Translation.MENU_SELECTION_EXPORT_IMAGE);
	private final MenuItem clearSelectionCache = UIFactory.menuItem(Translation.MENU_SELECTION_CLEAR_CACHE);
	private final MenuItem filterChunks = UIFactory.menuItem(Translation.MENU_TOOLS_FILTER_CHUNKS);
	private final MenuItem changeFields = UIFactory.menuItem(Translation.MENU_TOOLS_CHANGE_FIELDS);
	private final MenuItem editNBT = UIFactory.menuItem(Translation.MENU_TOOLS_EDIT_NBT);
	private final MenuItem swapChunks = UIFactory.menuItem(Translation.MENU_TOOLS_SWAP_CHUNKS);
	private final MenuItem editOverlays = UIFactory.menuItem(Translation.MENU_TOOLS_EDIT_OVERLAYS);
	private final MenuItem nextOverlay = UIFactory.menuItem(Translation.MENU_TOOLS_NEXT_OVERLAY);
	private final MenuItem nextOverlayType = UIFactory.menuItem(Translation.MENU_TOOLS_NEXT_OVERLAY_TYPE);
	private final MenuItem sumSelection = UIFactory.menuItem(Translation.MENU_TOOLS_SUM_SELECTION);

	private int previousSelectedChunks = 0;
	private boolean previousInvertedSelection = false;
	private final IntegerProperty heightValue = new SimpleIntegerProperty(319);
	private final BooleanProperty heightDisabled = new SimpleBooleanProperty(false);

	public OptionBar(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("option-bar-box");
		menuBar.getStyleClass().add("option-bar");

		getStylesheets().add(OptionBar.class.getClassLoader().getResource("style/component/option-bar.css").toExternalForm());

		tileMap.setOnUpdate(this::onUpdate);

		file.getItems().addAll(
				openWorld,
				openDimension,
				openRecent, UIFactory.separator(),
				settings, renderSettings, UIFactory.separator(),
				quit);
		view.getItems().addAll(
				reload, UIFactory.separator(),
				chunkGrid, regionGrid, coordinates, UIFactory.separator(),
				goTo, resetZoom, UIFactory.separator(),
				saveScreenshot, UIFactory.separator(),
				clearViewCache, clearAllCache);
		selection.getItems().addAll(
				clear, invert, invertRegions, UIFactory.separator(),
				copy, paste, UIFactory.separator(),
				exportChunks, delete, UIFactory.separator(),
				importSelection, exportSelection, UIFactory.separator(),
				exportImage, UIFactory.separator(),
				clearSelectionCache);
		tools.getItems().addAll(
				importChunks, filterChunks, changeFields, editNBT, UIFactory.separator(),
				swapChunks, UIFactory.separator(),
				editOverlays, nextOverlay, nextOverlayType, sumSelection);
		about.setOnMouseClicked(e -> DialogHelper.showAboutDialog(primaryStage));
		Menu aboutMenu = new Menu();
		aboutMenu.setGraphic(about);

		hSlider.getStyleClass().add("option-bar-slider-box");

		heightValue.bind(hSlider.valueProperty());

		heightValue.addListener((v, o, n) -> {
			if (!tileMap.getDisabled()) {
				heightDisabled.set(true);
				ConfigProvider.WORLD.setRenderHeight(n.intValue());
				CacheHelper.clearAllCacheAsync(tileMap, () -> {
					heightDisabled.set(false);
					if (hSlider.getValue() != heightValue.get()) {
						heightValue.set(hSlider.getValue());
					}
				});
			}
		});

		// when we press escape we want to give the focus back to the tile map
		setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				tileMap.requestFocus();
			}
		});

		menuBar.getMenus().addAll(file, view, selection, tools, aboutMenu);

		openWorld.setOnAction(e -> DialogHelper.openWorld(tileMap, primaryStage));
		settings.setOnAction(e -> DialogHelper.editSettings(tileMap, primaryStage, false));
		renderSettings.setOnAction(e -> DialogHelper.editSettings(tileMap, primaryStage, true));
		quit.setOnAction(e -> DialogHelper.quit(tileMap, primaryStage));
		reload.setOnAction(e -> tileMap.reload());
		chunkGrid.setOnAction(e -> tileMap.setShowChunkGrid(chunkGrid.isSelected()));
		regionGrid.setOnAction(e -> tileMap.setShowRegionGrid(regionGrid.isSelected()));
		coordinates.setOnAction(e -> tileMap.setShowCoordinates(coordinates.isSelected()));
		goTo.setOnAction(e -> DialogHelper.gotoCoordinate(tileMap, primaryStage));
		resetZoom.setOnAction(e -> tileMap.setScale(1));
		saveScreenshot.setOnAction(e -> DialogHelper.screenshot(tileMap, primaryStage));
		clearAllCache.setOnAction(e -> CacheHelper.clearAllCache(tileMap));
		clearViewCache.setOnAction(e -> CacheHelper.clearViewCache(tileMap));
		clear.setOnAction(e -> tileMap.clearSelection());
		invert.setOnAction(e -> tileMap.invertSelection());
		invertRegions.setOnAction(e -> tileMap.invertRegionsWithSelection());
		copy.setOnAction(e -> DialogHelper.copySelectedChunks(tileMap));
		paste.setOnAction(e -> DialogHelper.pasteSelectedChunks(tileMap, primaryStage));
		exportChunks.setOnAction(e -> DialogHelper.exportSelectedChunks(tileMap, primaryStage));
		importChunks.setOnAction(e -> DialogHelper.importChunks(tileMap, primaryStage));
		delete.setOnAction(e -> DialogHelper.deleteSelection(tileMap, primaryStage));
		importSelection.setOnAction(e -> DialogHelper.importSelection(tileMap, primaryStage));
		exportSelection.setOnAction(e -> DialogHelper.exportSelection(tileMap, primaryStage));
		exportImage.setOnAction(e -> DialogHelper.generateImageFromSelection(tileMap, primaryStage));
		clearSelectionCache.setOnAction(e -> CacheHelper.clearSelectionCache(tileMap));
		filterChunks.setOnAction(e -> DialogHelper.filterChunks(tileMap, primaryStage));
		changeFields.setOnAction(e -> DialogHelper.changeFields(tileMap, primaryStage));
		editNBT.setOnAction(e -> DialogHelper.editNBT(tileMap, primaryStage));
		swapChunks.setOnAction(e -> DialogHelper.swapChunks(tileMap, primaryStage));
		editOverlays.setOnAction(e -> DialogHelper.editOverlays(tileMap, primaryStage));
		nextOverlay.setOnAction(e -> tileMap.nextOverlay());
		nextOverlayType.setOnAction(e -> tileMap.nextOverlayType());
		sumSelection.setOnAction(e -> DialogHelper.sumSelection(tileMap, primaryStage));


		openWorld.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN));
		settings.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCodeCombination.SHORTCUT_DOWN));
		renderSettings.setAccelerator(new KeyCodeCombination(KeyCode.E));
		quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCodeCombination.SHORTCUT_DOWN));
		reload.setAccelerator(new KeyCodeCombination(KeyCode.F5));
		chunkGrid.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCodeCombination.SHORTCUT_DOWN));
		regionGrid.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCodeCombination.SHORTCUT_DOWN));
		coordinates.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCodeCombination.SHORTCUT_DOWN));
		goTo.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCodeCombination.SHORTCUT_DOWN));
		resetZoom.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCodeCombination.SHORTCUT_DOWN));
		saveScreenshot.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCodeCombination.SHORTCUT_DOWN));
		clearAllCache.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		clearViewCache.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCodeCombination.SHORTCUT_DOWN));
		clear.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCodeCombination.SHORTCUT_DOWN));
		invert.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCodeCombination.SHORTCUT_DOWN));
		invertRegions.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN));
		paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN));
		exportChunks.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		importChunks.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		delete.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCodeCombination.SHORTCUT_DOWN));
		importSelection.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCodeCombination.SHORTCUT_DOWN));
		exportSelection.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN));
		clearSelectionCache.setAccelerator(new KeyCodeCombination(KeyCode.J, KeyCodeCombination.SHORTCUT_DOWN));
		filterChunks.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCodeCombination.SHORTCUT_DOWN));
		changeFields.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCodeCombination.SHORTCUT_DOWN));
		editNBT.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCodeCombination.SHORTCUT_DOWN));
		swapChunks.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCodeCombination.SHORTCUT_DOWN));
		editOverlays.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCodeCombination.SHORTCUT_DOWN));
		nextOverlay.setAccelerator(new KeyCodeCombination(KeyCode.O));
		nextOverlayType.setAccelerator(new KeyCodeCombination(KeyCode.N));

		setSelectionDependentMenuItemsEnabled(tileMap, tileMap.getSelectedChunks(), tileMap.getSelection().isInverted());
		setWorldDependentMenuItemsEnabled(false, tileMap, primaryStage);

		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(e -> paste.setDisable(!hasValidClipboardContent(tileMap) || tileMap.getDisabled()));

		setLeft(menuBar);
		setRight(hSlider);

		checkForUpdateAsync();
	}

	private void checkForUpdateAsync() {
		new Thread(() -> {
			String applicationVersion;
			try {
				applicationVersion = FileHelper.getManifestAttributes().getValue("Application-Version");
			} catch (IOException ex) {
				// don't check for updates if we're in dev mode
				return;
			}

			VersionChecker checker = new VersionChecker("Querz", "mcaselector");
			VersionChecker.VersionData data;
			try {
				data = checker.fetchLatestVersion();
			} catch (Exception e) {
				// don't show update possibility when we failed to check the version
				return;
			}

			if (data != null && data.isNewerThan(applicationVersion)) {
				Platform.runLater(() -> {
					Hyperlink download = UIFactory.hyperlink("Update", data.getLink(), null);
					download.getStyleClass().add("hyperlink-menu-update");
					Menu updateMenu = new Menu();
					updateMenu.setGraphic(download);
					menuBar.getMenus().add(updateMenu);
				});
			}
		}).start();
	}

	private void onUpdate(TileMap tileMap) {
		int selectedChunks = tileMap.getSelectedChunks();
		boolean invertedSelection = tileMap.getSelection().isInverted();
		if (previousSelectedChunks != selectedChunks || previousInvertedSelection != invertedSelection) {
			setSelectionDependentMenuItemsEnabled(tileMap, selectedChunks, invertedSelection);
		}
		previousSelectedChunks = selectedChunks;
		previousInvertedSelection = invertedSelection;
		nextOverlay.setDisable(tileMap.getOverlay() == null);
		sumSelection.setDisable(tileMap.getOverlay() == null || tileMap.getSelectedChunks() == 0);
	}

	public void setWorldDependentMenuItemsEnabled(boolean enabled, TileMap tileMap, Stage primaryStage) {
		renderSettings.setDisable(!enabled);
		clearViewCache.setDisable(!enabled);
		clearAllCache.setDisable(!enabled);
		saveScreenshot.setDisable(!enabled);
		filterChunks.setDisable(!enabled);
		changeFields.setDisable(!enabled);
		importChunks.setDisable(!enabled);
		invert.setDisable(!enabled);
		invertRegions.setDisable(!enabled || tileMap.getSelectedChunks() == 0);
		paste.setDisable(!enabled || !hasValidClipboardContent(tileMap));
		nextOverlay.setDisable(!enabled);
		nextOverlayType.setDisable(!enabled);
		sumSelection.setDisable(!enabled || tileMap.getOverlay() == null || tileMap.getSelectedChunks() == 0);
		hSlider.setDisable(!enabled);
		openDimension.getItems().clear();

		if (enabled && ConfigProvider.WORLD.getDimensionDirectories() != null && ConfigProvider.WORLD.getDimensionDirectories().size() > 1) {
			File currentWorldDir = ConfigProvider.WORLD.getRegionDir().getParentFile();
			ConfigProvider.WORLD.getDimensionDirectories().forEach(f -> {
				if (!f.equals(currentWorldDir)) {
					MenuItem dimItem = new MenuItem(f.getName());
					dimItem.setMnemonicParsing(false);
					dimItem.setOnAction(e -> DialogHelper.setWorld(FileHelper.detectWorldDirectories(f), ConfigProvider.WORLD.getDimensionDirectories(), tileMap, primaryStage));
					openDimension.getItems().add(dimItem);
				}
			});
		}

		openDimension.setDisable(!enabled || openDimension.getItems().size() == 0);

		if (ConfigProvider.GLOBAL.getRecentWorlds().size() > 0) {
			openRecent.getItems().clear();
			File currentWorld = ConfigProvider.WORLD.getRegionDir();
			ConfigProvider.GLOBAL.getRecentWorlds().descendingMap().forEach((k, v) -> {
				if (currentWorld == null || !v.recentWorld().equals(currentWorld.getParentFile())) {
					MenuItem openRecentItem = new MenuItem(v.toString());
					openRecentItem.setMnemonicParsing(false);
					openRecentItem.setOnAction(e -> DialogHelper.setWorld(FileHelper.detectWorldDirectories(v.recentWorld()), v.dimensionDirectories(), tileMap, primaryStage));
					openRecent.getItems().add(openRecentItem);
				}
			});
			openRecent.getItems().add(UIFactory.separator());
			MenuItem clear = UIFactory.menuItem(Translation.MENU_FILE_OPEN_RECENT_WORLD_CLEAR);
			clear.setOnAction(e -> {
				openRecent.getItems().clear();
				ConfigProvider.GLOBAL.getRecentWorlds().clear();
				openRecent.setDisable(true);
			});
			openRecent.getItems().add(clear);
		}

		openRecent.setDisable(openRecent.getItems().size() <= 2); // "empty" means it either has no items or it only has the separator and the clear button
	}

	public void setEditOverlaysEnabled(boolean enabled) {
		editOverlays.setDisable(!enabled);
	}

	private void setSelectionDependentMenuItemsEnabled(TileMap tileMap, int selected, boolean inverted) {
		clear.setDisable(selected == 0 && !inverted);
		exportChunks.setDisable(selected == 0 && !inverted);
		exportSelection.setDisable(selected == 0 && !inverted);
		exportImage.setDisable(selected == 0 && !inverted);
		delete.setDisable(selected == 0 && !inverted);
		clearSelectionCache.setDisable(selected == 0 && !inverted);
		editNBT.setDisable(selected != 1 || inverted);
		swapChunks.setDisable(selected != 2 || inverted);
		copy.setDisable(selected == 0 && !inverted);
		invertRegions.setDisable(selected == 0 || inverted);
		sumSelection.setDisable((tileMap.getOverlay() == null || selected == 0));
	}

	private boolean hasValidClipboardContent(TileMap tileMap) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable content = clipboard.getContents(tileMap);
		DataFlavor[] flavors = content.getTransferDataFlavors();
		return flavors.length == 1 && flavors[0].equals(ClipboardSelection.SELECTION_DATA_FLAVOR);
	}

	public void setRenderHeight(int height) {
		hSlider.valueProperty().set(height);
	}
}
