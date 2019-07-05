package net.querz.mcaselector.ui;

import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Translation;
import net.querz.mcaselector.util.UIFactory;

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
	private MenuItem changeFields = UIFactory.menuItem(Translation.MENU_TOOLS_FILTER_CHUNKS);

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
		tools.getItems().addAll(importChunks, filterChunks, changeFields);
		about.setOnMouseClicked(e -> Helper.showAboutDialog(tileMap, primaryStage));
		Menu aboutMenu = new Menu();
		aboutMenu.setGraphic(about);

		getMenus().addAll(file, view, selection, tools, aboutMenu);

		open.setOnAction(e -> Helper.openWorld(tileMap, primaryStage, this));
		settings.setOnAction(e -> Helper.editSettings(tileMap, primaryStage));
		quit.setOnAction(e -> System.exit(0));
		chunkGrid.setOnAction(e -> tileMap.setShowChunkGrid(chunkGrid.isSelected()));
		regionGrid.setOnAction(e -> tileMap.setShowRegionGrid(regionGrid.isSelected()));
		goTo.setOnAction(e -> Helper.gotoCoordinate(tileMap, primaryStage));
		clearAllCache.setOnAction(e -> Helper.clearAllCache(tileMap));
		clearViewCache.setOnAction(e -> Helper.clearViewCache(tileMap));
		clear.setOnAction(e -> tileMap.clearSelection());
		exportChunks.setOnAction(e -> Helper.exportSelectedChunks(tileMap, primaryStage));
		importChunks.setOnAction(e -> Helper.importChunks(tileMap, primaryStage));
		delete.setOnAction(e -> Helper.deleteSelection(tileMap, primaryStage));
		importSelection.setOnAction(e -> Helper.importSelection(tileMap, primaryStage));
		exportSelection.setOnAction(e -> Helper.exportSelection(tileMap, primaryStage));
		clearSelectionCache.setOnAction(e -> Helper.clearSelectionCache(tileMap));
		filterChunks.setOnAction(e -> Helper.filterChunks(tileMap, primaryStage));
		changeFields.setOnAction(e -> Helper.changeFields(tileMap, primaryStage));

		open.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
		chunkGrid.setAccelerator(KeyCombination.keyCombination("Ctrl+R"));
		regionGrid.setAccelerator(KeyCombination.keyCombination("Ctrl+T"));
		goTo.setAccelerator(KeyCombination.keyCombination("Ctrl+G"));
		clearAllCache.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+K"));
		clearViewCache.setAccelerator(KeyCombination.keyCombination("Ctrl+K"));
		clear.setAccelerator(KeyCombination.keyCombination("Ctrl+L"));
		exportChunks.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+E"));
		importChunks.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+I"));
		delete.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));
		importSelection.setAccelerator(KeyCombination.keyCombination("Ctrl+I"));
		exportSelection.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
		clearSelectionCache.setAccelerator(KeyCombination.keyCombination("Ctrl+J"));
		filterChunks.setAccelerator(KeyCombination.keyCombination("Ctrl+F"));
		changeFields.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));

		setSelectionDependentMenuItemsEnabled(false);
		setWorldDependentMenuItemsEnabled(false);
	}

	private void onUpdate(TileMap tileMap) {
		int selectedChunks = tileMap.getSelectedChunks();
		if (previousSelectedChunks != 0 && selectedChunks == 0
				|| previousSelectedChunks == 0 && selectedChunks != 0) {
			setSelectionDependentMenuItemsEnabled(selectedChunks != 0);
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
}
