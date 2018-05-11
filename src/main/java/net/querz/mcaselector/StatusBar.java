package net.querz.mcaselector;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;

public class StatusBar extends BorderPane {
	private GridPane grid = new GridPane();
	private Label selectedChunks = new Label("selected: 0");
	private Label hoveredBlock = new Label("block: 0, 0");
	private Label hoveredChunk = new Label("chunk: 0, 0");
	private Label hoveredRegion = new Label("region: 0, 0");

	private static final Color textColor = Color.WHITE;

	public StatusBar(TileMap tileMap) {
		setBackground(new Background(new BackgroundFill(new Color(0.15, 0.15, 0.15, 1), CornerRadii.EMPTY, Insets.EMPTY)));

		tileMap.setOnUpdate(this::update);
		tileMap.setOnHover(this::update);
		for (int i = 0; i < 4; i++) {
			grid.getColumnConstraints().add(new ColumnConstraints(120, 120, 120));
		}
		hoveredBlock.setTextFill(textColor);
		hoveredChunk.setTextFill(textColor);
		hoveredRegion.setTextFill(textColor);
		selectedChunks.setTextFill(textColor);
		grid.add(hoveredBlock, 0, 0, 1, 1);
		grid.add(hoveredChunk, 1, 0, 1, 1);
		grid.add(hoveredRegion, 2, 0, 1 ,1);
		grid.add(selectedChunks, 3, 0, 1, 1);
		setLeft(grid);
	}

	private void update(TileMap tileMap) {
		selectedChunks.setText("selected: " + tileMap.getSelectedChunks());
		Point2i b = tileMap.getHoveredBlock();
		if (b != null) {
			hoveredBlock.setText("block: " + b.getX() + ", " + b.getY());
			Point2i c = Helper.blockToChunk(b);
			hoveredChunk.setText("chunk: " + c.getX() + ", " + c.getY());
			Point2i r = Helper.blockToRegion(b);
			hoveredRegion.setText("region: " + r.getX() + ", " + r.getY());
		}
	}
}
