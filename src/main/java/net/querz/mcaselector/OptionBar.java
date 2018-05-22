package net.querz.mcaselector;

import javafx.scene.control.*;
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

	private Menu file = new Menu("File");
	private Menu view = new Menu("View");
	private Menu selection = new Menu("Selection");

	private MenuItem open = new MenuItem("Open");
	private MenuItem quit = new MenuItem("Quit");
	private CheckMenuItem chunkGrid = new CheckMenuItem("Chunk Grid");
	private CheckMenuItem regionGrid = new CheckMenuItem("Region Grid");
	private MenuItem goTo = new MenuItem("Goto");
	private MenuItem clearViewCache = new MenuItem("Clear cache");
	private MenuItem clearAllCache = new MenuItem("Clear all cache");
	private MenuItem clear = new MenuItem("Clear");
	private MenuItem exportChunks = new MenuItem("Export chunks");
	private MenuItem delete = new MenuItem("Delete chunks");
	private MenuItem importSelection = new MenuItem("Import selection");
	private MenuItem exportSelection = new MenuItem("Export selection");
	private MenuItem clearSelectionCache = new MenuItem("Clear cache");

	public OptionBar(TileMap tileMap, Stage primaryStage) {
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
		chunkGrid.setSelected(true);
		regionGrid.setSelected(true);

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
	}

	private void onUpdate(TileMap tileMap) {
		if (tileMap.getSelectedChunks() == 0) {
			clear.setDisable(true);
			exportChunks.setDisable(true);
			exportSelection.setDisable(true);
			delete.setDisable(true);

		} else {
			clear.setDisable(false);
			exportChunks.setDisable(false);
			exportSelection.setDisable(false);
			delete.setDisable(false);
		}
	}

	private SeparatorMenuItem separator() {
		return new SeparatorMenuItem();
	}
}
