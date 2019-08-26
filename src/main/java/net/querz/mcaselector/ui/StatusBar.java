package net.querz.mcaselector.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Translation;
import net.querz.mcaselector.util.Point2i;

public class StatusBar extends BorderPane {

	private GridPane grid = new GridPane();
	private Label selectedChunks = new Label(Translation.STATUS_SELECTED + ": 0");
	private Label hoveredRegion = new Label(Translation.STATUS_REGION + ": -, -");
	private Label hoveredChunk = new Label(Translation.STATUS_CHUNK + ": -, -");
	private Label hoveredBlock = new Label(Translation.STATUS_BLOCK + ": -, -");
	private Label visibleRegions = new Label(Translation.STATUS_VISIBLE + ": 0");
	private Label totalRegions = new Label(Translation.STATUS_TOTAL + ": 0");

	public StatusBar(TileMap tileMap) {
		getStyleClass().add("status-bar");

		tileMap.setOnUpdate(this::update);
		tileMap.setOnHover(this::update);
		for (int i = 0; i < 6; i++) {
			grid.getColumnConstraints().add(new ColumnConstraints(120, 120, 200));
		}
		hoveredRegion.setTooltip(new Tooltip(Translation.STATUS_REGION_TOOLTIP.toString()));
		hoveredChunk.setTooltip(new Tooltip(Translation.STATUS_CHUNK_TOOLTIP.toString()));
		hoveredBlock.setTooltip(new Tooltip(Translation.STATUS_BLOCK_TOOLTIP.toString()));
		selectedChunks.setTooltip(new Tooltip(Translation.STATUS_SELECTED_TOOLTIP.toString()));
		visibleRegions.setTooltip(new Tooltip(Translation.STATUS_VISIBLE_TOOLTIP.toString()));
		totalRegions.setTooltip(new Tooltip(Translation.STATUS_TOTAL_TOOLTIP.toString()));
		grid.add(hoveredRegion, 0, 0, 1 ,1);
		grid.add(hoveredChunk, 1, 0, 1, 1);
		grid.add(hoveredBlock, 2, 0, 1, 1);
		grid.add(selectedChunks, 3, 0, 1, 1);
		grid.add(visibleRegions, 4, 0, 1, 1);
		grid.add(totalRegions, 5, 0, 1, 1);
		setLeft(grid);
	}

	private void update(TileMap tileMap) {
		selectedChunks.setText(Translation.STATUS_SELECTED + ": " + tileMap.getSelectedChunks());
		visibleRegions.setText(Translation.STATUS_VISIBLE + ": " + tileMap.getVisibleTiles());
		totalRegions.setText(Translation.STATUS_TOTAL + ": " + tileMap.getLoadedTiles());
		Point2i b = tileMap.getHoveredBlock();
		if (b != null) {
			hoveredBlock.setText(Translation.STATUS_BLOCK + ": " + b.getX() + ", " + b.getY());
			Point2i c = b.blockToChunk();
			hoveredChunk.setText(Translation.STATUS_CHUNK + ": " + c.getX() + ", " + c.getY());
			Point2i r = b.blockToRegion();
			hoveredRegion.setText(Translation.STATUS_REGION + ": " + r.getX() + ", " + r.getY());
		} else {
			hoveredBlock.setText(Translation.STATUS_BLOCK + ": -, -");
			hoveredChunk.setText(Translation.STATUS_CHUNK + ": -, -");
			hoveredRegion.setText(Translation.STATUS_REGION + ": -, -");
		}
	}
}
