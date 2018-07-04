package net.querz.mcaselector.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.*;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;

public class StatusBar extends BorderPane {

	private GridPane grid = new GridPane();
	private Label selectedChunks = new Label("selected: 0");
	private Label hoveredBlock = new Label("block: -, -");
	private Label hoveredChunk = new Label("chunk: -, -");
	private Label hoveredRegion = new Label("region: -, -");
	private Label visibleRegions = new Label("visible regions: 0");
	private Label totalRegions = new Label("total regions: 0");


	public StatusBar(TileMap tileMap) {
		getStyleClass().add("status-bar");

		tileMap.setOnUpdate(this::update);
		tileMap.setOnHover(this::update);
		for (int i = 0; i < 6; i++) {
			grid.getColumnConstraints().add(new ColumnConstraints(120, 120, 200));
		}
		grid.add(hoveredBlock, 0, 0, 1, 1);
		grid.add(hoveredChunk, 1, 0, 1, 1);
		grid.add(hoveredRegion, 2, 0, 1 ,1);
		grid.add(selectedChunks, 3, 0, 1, 1);
		grid.add(visibleRegions, 4, 0, 1, 1);
		grid.add(totalRegions, 5, 0, 1, 1);
		setLeft(grid);
	}

	private void update(TileMap tileMap) {
		selectedChunks.setText("selected: " + tileMap.getSelectedChunks());
		visibleRegions.setText("visible regions: " + tileMap.getVisibleTiles());
		totalRegions.setText("total regions: " + tileMap.getLoadedTiles());
		Point2i b = tileMap.getHoveredBlock();
		if (b != null) {
			hoveredBlock.setText("block: " + b.getX() + ", " + b.getY());
			Point2i c = Helper.blockToChunk(b);
			hoveredChunk.setText("chunk: " + c.getX() + ", " + c.getY());
			Point2i r = Helper.blockToRegion(b);
			hoveredRegion.setText("region: " + r.getX() + ", " + r.getY());
		} else {
			hoveredBlock.setText("block: -, -");
			hoveredChunk.setText("chunk: -, -");
			hoveredRegion.setText("region: -, -");
		}
	}
}
