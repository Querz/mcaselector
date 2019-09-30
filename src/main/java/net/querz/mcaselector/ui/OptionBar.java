package net.querz.mcaselector.ui;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.io.CacheHelper;
import net.querz.mcaselector.text.Translation;

public class OptionBar extends MenuBar {
	/*
	* File			View				Selection					Tools				About
	* - Open		- Chunk Grid		- Clear selection			- Import chunks
	* - Settings	- Region Grid		- Export selected chunks	- Filter chunks
	* - Quit		- Goto				- Delete selected chunks	- Change fields
	*				- Clear cache		- Import selection
	*				- Clear all cache	- Export selection
	*									- Clear cache
	* */

	private Menu file = UIFactory.menu(Translation.MENU_FILE);
	private Menu view = UIFactory.menu(Translation.MENU_VIEW);
	private Menu selection = UIFactory.menu(Translation.MENU_SELECTION);
	private Menu tools = UIFactory.menu(Translation.MENU_TOOLS);
	private Label about = UIFactory.label(Translation.MENU_ABOUT);

	private MenuItem open = UIFactory.menuItem(Translation.MENU_FILE_OPEN);
	private MenuItem settings = UIFactory.menuItem(Translation.MENU_FILE_SETTINGS);
	private MenuItem quit = UIFactory.menuItem(Translation.MENU_FILE_QUIT);
	private CheckMenuItem chunkGrid = UIFactory.checkMenuItem(Translation.MENU_VIEW_CHUNK_GRID, true);
	private CheckMenuItem regionGrid = UIFactory.checkMenuItem(Translation.MENU_VIEW_REGION_GRID, true);
	private MenuItem goTo = UIFactory.menuItem(Translation.MENU_VIEW_GOTO);
	private MenuItem clearViewCache = UIFactory.menuItem(Translation.MENU_VIEW_CLEAR_CACHE);
	private MenuItem clearAllCache = UIFactory.menuItem(Translation.MENU_VIEW_CLEAR_ALL_CACHE);
	private MenuItem clear = UIFactory.menuItem(Translation.MENU_SELECTION_CLEAR);
	private MenuItem exportChunks = UIFactory.menuItem(Translation.MENU_SELECTION_EXPORT_CHUNKS);
	private MenuItem importChunks = UIFactory.menuItem(Translation.MENU_TOOLS_IMPORT_CHUNKS);
	private MenuItem delete = UIFactory.menuItem(Translation.MENU_SELECTION_DELETE_CHUNKS);
	private MenuItem importSelection = UIFactory.menuItem(Translation.MENU_SELECTION_IMPORT_SELECTION);
	private MenuItem exportSelection = UIFactory.menuItem(Translation.MENU_SELECTION_EXPORT_SELECTION);
	private MenuItem clearSelectionCache = UIFactory.menuItem(Translation.MENU_SELECTION_CLEAR_CACHE);
	private MenuItem filterChunks = UIFactory.menuItem(Translation.MENU_TOOLS_FILTER_CHUNKS);
	private MenuItem changeFields = UIFactory.menuItem(Translation.MENU_TOOLS_CHANGE_FIELDS);
	private MenuItem editNBT = UIFactory.menuItem(Translation.MENU_TOOLS_CHANGE_FIELDS);

	private int previousSelectedChunks = 0;

	public OptionBar(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("option-bar");

		tileMap.setOnUpdate(this::onUpdate);

		file.getItems().addAll(open, settings, UIFactory.separator(), quit);
		view.getItems().addAll(
				chunkGrid, regionGrid, UIFactory.separator(),
				goTo, UIFactory.separator(),
				clearViewCache, clearAllCache);
		selection.getItems().addAll(
				clear, UIFactory.separator(),
				exportChunks, delete, UIFactory.separator(),
				importSelection, exportSelection, UIFactory.separator(),
				clearSelectionCache);
		tools.getItems().addAll(importChunks, filterChunks, changeFields, editNBT);
		about.setOnMouseClicked(e -> DialogHelper.showAboutDialog(primaryStage));
		Menu aboutMenu = new Menu();
		aboutMenu.setGraphic(about);

		getMenus().addAll(file, view, selection, tools, aboutMenu);

		open.setOnAction(e -> DialogHelper.openWorld(tileMap, primaryStage, this));
		settings.setOnAction(e -> DialogHelper.editSettings(tileMap, primaryStage));
		quit.setOnAction(e -> System.exit(0));
		chunkGrid.setOnAction(e -> tileMap.setShowChunkGrid(chunkGrid.isSelected()));
		regionGrid.setOnAction(e -> tileMap.setShowRegionGrid(regionGrid.isSelected()));
		goTo.setOnAction(e -> DialogHelper.gotoCoordinate(tileMap, primaryStage));
		clearAllCache.setOnAction(e -> CacheHelper.clearAllCache(tileMap));
		clearViewCache.setOnAction(e -> CacheHelper.clearViewCache(tileMap));
		clear.setOnAction(e -> tileMap.clearSelection());
		exportChunks.setOnAction(e -> DialogHelper.exportSelectedChunks(tileMap, primaryStage));
		importChunks.setOnAction(e -> DialogHelper.importChunks(tileMap, primaryStage));
		delete.setOnAction(e -> DialogHelper.deleteSelection(tileMap, primaryStage));
		importSelection.setOnAction(e -> DialogHelper.importSelection(tileMap, primaryStage));
		exportSelection.setOnAction(e -> DialogHelper.exportSelection(tileMap, primaryStage));
		clearSelectionCache.setOnAction(e -> CacheHelper.clearSelectionCache(tileMap));
		filterChunks.setOnAction(e -> DialogHelper.filterChunks(tileMap, primaryStage));
		changeFields.setOnAction(e -> DialogHelper.changeFields(tileMap, primaryStage));
		editNBT.setOnAction(e -> DialogHelper.editNBT(tileMap, primaryStage));

		open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN));
		quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCodeCombination.SHORTCUT_DOWN));
		chunkGrid.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCodeCombination.SHORTCUT_DOWN));
		regionGrid.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCodeCombination.SHORTCUT_DOWN));
		goTo.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCodeCombination.SHORTCUT_DOWN));
		clearAllCache.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		clearViewCache.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCodeCombination.SHORTCUT_DOWN));
		clear.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCodeCombination.SHORTCUT_DOWN));
		exportChunks.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		importChunks.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		delete.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCodeCombination.SHORTCUT_DOWN));
		importSelection.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCodeCombination.SHORTCUT_DOWN));
		exportSelection.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN));
		clearSelectionCache.setAccelerator(new KeyCodeCombination(KeyCode.J, KeyCodeCombination.SHORTCUT_DOWN));
		filterChunks.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCodeCombination.SHORTCUT_DOWN));
		changeFields.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCodeCombination.SHORTCUT_DOWN));

		setSelectionDependentMenuItemsEnabled(false);
		setWorldDependentMenuItemsEnabled(false);
		setSingleSelectionDependentMenuItemsEnabled(false);
	}

	private void onUpdate(TileMap tileMap) {
		int selectedChunks = tileMap.getSelectedChunks();
		if (previousSelectedChunks != 0 && selectedChunks == 0
				|| previousSelectedChunks == 0 && selectedChunks != 0) {
			setSelectionDependentMenuItemsEnabled(selectedChunks != 0);
		}
		if (previousSelectedChunks != 1 && selectedChunks == 1
				|| previousSelectedChunks == 1 && selectedChunks != 1) {
			setSingleSelectionDependentMenuItemsEnabled(selectedChunks == 1);
		}
		previousSelectedChunks = selectedChunks;
	}

	public void setWorldDependentMenuItemsEnabled(boolean enabled) {
		filterChunks.setDisable(!enabled);
		changeFields.setDisable(!enabled);
		importChunks.setDisable(!enabled);
	}

	private void setSelectionDependentMenuItemsEnabled(boolean enabled) {
		clear.setDisable(!enabled);
		exportChunks.setDisable(!enabled);
		exportSelection.setDisable(!enabled);
		delete.setDisable(!enabled);
		clearSelectionCache.setDisable(!enabled);
	}

	private void setSingleSelectionDependentMenuItemsEnabled(boolean enabled) {
		editNBT.setDisable(!enabled);
	}
}
