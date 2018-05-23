package net.querz.mcaselector;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Helper;

import static javafx.scene.input.KeyCombination.*;

public class OptionBar extends MenuBar {
	/*
	* File		View				Selection
	* - Open	- Chunk Grid		- Clear
	* - Quit	- Region Grid		- Export chunks
	*			- Goto				- Delete chunks
	*			- Clear cache		- Import selection
	*			- Clear all cache	- Export selection
	*								- Clear cache
	* */

	private Menu file = menu("File");
	private Menu view = menu("View");
	private Menu selection = menu("Selection");

	private MenuItem open = menuItem("Open");
	private MenuItem quit = menuItem("Quit");
	private CheckMenuItem chunkGrid = checkMenuItem("Chunk Grid", true);
	private CheckMenuItem regionGrid = checkMenuItem("Region Grid", true);
	private MenuItem goTo = menuItem("Goto");
	private MenuItem clearViewCache = menuItem("Clear cache");
	private MenuItem clearAllCache = menuItem("Clear all cache");
	private MenuItem clear = menuItem("Clear");
	private MenuItem exportChunks = menuItem("Export chunks");
	private MenuItem delete = menuItem("Delete chunks");
	private MenuItem importSelection = menuItem("Import selection");
	private MenuItem exportSelection = menuItem("Export selection");
	private MenuItem clearSelectionCache = menuItem("Clear cache");

	public OptionBar(TileMap tileMap, Stage primaryStage) {
		setId("option-bar");
		tileMap.setOnUpdate(this::onUpdate);

		getMenus().addAll(file, view, selection);
		file.getItems().addAll(open, quit);
		view.getItems().addAll(
				chunkGrid, regionGrid, separator(),
				goTo, separator(),
				clearViewCache, clearAllCache);
		selection.getItems().addAll(
				clear, separator(),
				exportChunks, delete, separator(),
				importSelection, exportSelection, separator(),
				clearSelectionCache);

		open.setOnAction(e -> Helper.openWorld(tileMap, primaryStage));
		quit.setOnAction(e -> System.exit(0));
		chunkGrid.setOnAction(e -> tileMap.setShowChunkGrid(chunkGrid.isSelected()));
		regionGrid.setOnAction(e -> tileMap.setShowRegionGrid(regionGrid.isSelected()));
		goTo.setOnAction(e -> Helper.gotoCoordinate(tileMap));
		clearAllCache.setOnAction(e -> Helper.clearAllCache(tileMap));
		clearViewCache.setOnAction(e -> Helper.clearViewCache(tileMap));
		clear.setOnAction(e -> tileMap.clearSelection());
		exportChunks.setOnAction(e -> Helper.exportSelectedChunks(tileMap, primaryStage));
		delete.setOnAction(e -> Helper.deleteSelection(tileMap));
		importSelection.setOnAction(e -> Helper.importSelection(tileMap, primaryStage));
		exportSelection.setOnAction(e -> Helper.exportSelection(tileMap, primaryStage));
		clearSelectionCache.setOnAction(e -> Helper.clearSelectionCache(tileMap));

		open.setAccelerator(KeyCombination.keyCombination("Ctrl+O"));
		chunkGrid.setAccelerator(KeyCombination.keyCombination("Ctrl+R"));
		regionGrid.setAccelerator(KeyCombination.keyCombination("Ctrl+T"));
		goTo.setAccelerator(KeyCombination.keyCombination("Ctrl+G"));
		clearAllCache.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+K"));
		clearViewCache.setAccelerator(KeyCombination.keyCombination("Ctrl+K"));
		clear.setAccelerator(KeyCombination.keyCombination("Ctrl+L"));
		exportChunks.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+E"));
		delete.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));
		importSelection.setAccelerator(KeyCombination.keyCombination("Ctrl+I"));
		exportSelection.setAccelerator(KeyCombination.keyCombination("Ctrl+E"));
		clearSelectionCache.setAccelerator(KeyCombination.keyCombination("Ctrl+J"));
	}

	private void onUpdate(TileMap tileMap) {
		if (tileMap.getSelectedChunks() == 0) {
			clear.setDisable(true);
			clear.setId("menu-item-disabled");
			exportChunks.setDisable(true);
			exportChunks.setId("menu-item-disabled");
			exportSelection.setDisable(true);
			exportSelection.setId("menu-item-disabled");
			delete.setDisable(true);
			delete.setId("menu-item-disabled");

		} else {
			clear.setDisable(false);
			clear.setId("menu-item-enabled");
			exportChunks.setDisable(false);
			exportChunks.setId("menu-item-enabled");
			exportSelection.setDisable(false);
			exportSelection.setId("menu-item-enabled");
			delete.setDisable(false);
			delete.setId("menu-item-enabled");
		}
	}

	private Menu menu(String text) {
		Menu menu = new Menu(text);
		menu.setId("menu");
		return menu;
	}

	private MenuItem menuItem(String text) {
		MenuItem item = new MenuItem(text);
		item.setId("menu-item-enabled");
		return item;
	}

	private CheckMenuItem checkMenuItem(String text, boolean selected) {
		CheckMenuItem item = new CheckMenuItem(text);
		item.setId("check-menu-item-enabled");
		item.setSelected(selected);
		return item;
	}

	private SeparatorMenuItem separator() {
		return new SeparatorMenuItem();
	}
}
