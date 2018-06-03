package net.querz.mcaselector;

import com.sun.javafx.css.PseudoClassState;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Helper;

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

	private int previousSelectedChunks = 0;

	public OptionBar(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("option-bar");

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

		setMenuItemsEnabled(false);
	}

	private void onUpdate(TileMap tileMap) {
		int selectedChunks = tileMap.getSelectedChunks();
		if (previousSelectedChunks != 0 && selectedChunks == 0
				|| previousSelectedChunks == 0 && selectedChunks != 0) {
			setMenuItemsEnabled(selectedChunks != 0);
		}
		previousSelectedChunks = selectedChunks;
	}

	private void setMenuItemsEnabled(boolean enabled) {
		clear.setDisable(!enabled);
		exportChunks.setDisable(!enabled);
		exportSelection.setDisable(!enabled);
		delete.setDisable(!enabled);
		clearSelectionCache.setDisable(!enabled);
	}

	private Menu menu(String text) {
		return new Menu(text);
	}

	private MenuItem menuItem(String text) {
		MenuItem item = new MenuItem(text);
		return item;
	}

	private CheckMenuItem checkMenuItem(String text, boolean selected) {
		CheckMenuItem item = new CheckMenuItem(text);
		item.setSelected(selected);
		return item;
	}

	private SeparatorMenuItem separator() {
		return new SeparatorMenuItem();
	}
}
