package net.querz.mcaselector;

import javafx.scene.control.*;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Point2i;

import java.util.Optional;

public class OptionBar extends MenuBar {
	/*
	* File		View				Selection
	* - Open	- Chunk Grid		- Clear
	* - Quit	- Region Grid		- Delete
	*			- Goto
	* */

	private Menu file = new Menu("File");
	private Menu view = new Menu("View");
	private Menu selection = new Menu("Selection");

	private MenuItem open = new MenuItem("Open");
	private MenuItem quit = new MenuItem("Quit");
	private CheckMenuItem chunkGrid = new CheckMenuItem("Chunk Grid");
	private CheckMenuItem regionGrid = new CheckMenuItem("Region Grid");
	private MenuItem goTo = new MenuItem("Goto");
	private MenuItem clear = new MenuItem("Clear");
	private MenuItem delete = new MenuItem("Delete");

	private static final SeparatorMenuItem separator = new SeparatorMenuItem();

	public OptionBar(TileMap tileMap) {
		getMenus().addAll(file, view, selection);
		file.getItems().addAll(open, quit);
		view.getItems().addAll(chunkGrid, regionGrid, separator, goTo);
		selection.getItems().addAll(clear, delete);

		open.setOnAction(e -> {
			//TODO: open
		});

		quit.setOnAction(e -> System.exit(0));

		chunkGrid.setSelected(true);
		chunkGrid.setOnAction(e -> tileMap.setShowChunkGrid(chunkGrid.isSelected()));

		regionGrid.setSelected(true);
		regionGrid.setOnAction(e -> tileMap.setShowRegionGrid(regionGrid.isSelected()));

		goTo.setOnAction(e -> {
			Optional<Point2i> result = new GotoDialog().showAndWait();
			result.ifPresent(r -> tileMap.goTo(r.getX(), r.getY()));
		});

		clear.setOnAction(e -> tileMap.clearSelection());

		delete.setOnAction(e -> {
			//TODO: delete
		});
	}
}
