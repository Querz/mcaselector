package net.querz.mcaselector;

import javafx.scene.control.*;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Helper;

public class OptionBar extends MenuBar {
	/*
	* File		View				Selection
	* - Open	- Chunk Grid		- Clear
	* - Quit	- Region Grid		- Delete
	*			- Goto				- Clear cache
	*			- Clear cache
	*			- Clear all cache
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
	private MenuItem delete = new MenuItem("Delete");
	private MenuItem clearSelectionCache = new MenuItem("Clear cache");

	public OptionBar(TileMap tileMap, Stage primaryStage) {
		getMenus().addAll(file, view, selection);
		file.getItems().addAll(open, quit);
		view.getItems().addAll(chunkGrid, regionGrid, separator(), goTo, separator(), clearViewCache, clearAllCache);
		selection.getItems().addAll(clear, delete, separator(), clearSelectionCache);
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
		delete.setOnAction(e -> Helper.deleteSelection(tileMap));
		clearSelectionCache.setOnAction(e -> Helper.clearSelectionCache(tileMap));
	}

	private SeparatorMenuItem separator() {
		return new SeparatorMenuItem();
	}
}
