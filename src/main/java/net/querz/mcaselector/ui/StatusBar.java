package net.querz.mcaselector.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import net.querz.mcaselector.validation.ShutdownHooks;

public class StatusBar extends StackPane {

	private final GridPane grid = new GridPane();
	private final Label selectedChunks = new Label(Translation.STATUS_SELECTED + ": 0");
	private final Label hoveredRegion = new Label(Translation.STATUS_REGION + ": -, -");
	private final Label hoveredChunk = new Label(Translation.STATUS_CHUNK + ": -, -");
	private final Label hoveredBlock = new Label(Translation.STATUS_BLOCK + ": -, -");
	private final Label visibleRegions = new Label(Translation.STATUS_VISIBLE + ": 0");
	private final Label totalRegions = new Label(Translation.STATUS_TOTAL + ": 0");
	private final Label overlay = new Label(Translation.STATUS_OVERLAY + ": -");

	ImageView loadIcon = new ImageView(FileHelper.getIconFromResources("img/load"));
	BorderPane bp = new BorderPane();
	RotateTransition rt;

	public StatusBar(TileMap tileMap) {
		getStyleClass().add("status-bar");
		grid.getStyleClass().add("status-bar-grid");

		tileMap.setOnUpdate(this::update);
		tileMap.setOnHover(this::update);
		for (int i = 0; i < 6; i++) {
			ColumnConstraints constraints = new ColumnConstraints();
			constraints.setMinWidth(140);
			constraints.setFillWidth(true);
			grid.getColumnConstraints().add(constraints);
		}
		hoveredRegion.setTooltip(new Tooltip(Translation.STATUS_REGION_TOOLTIP.toString()));
		hoveredChunk.setTooltip(new Tooltip(Translation.STATUS_CHUNK_TOOLTIP.toString()));
		hoveredBlock.setTooltip(new Tooltip(Translation.STATUS_BLOCK_TOOLTIP.toString()));
		selectedChunks.setTooltip(new Tooltip(Translation.STATUS_SELECTED_TOOLTIP.toString()));
		visibleRegions.setTooltip(new Tooltip(Translation.STATUS_VISIBLE_TOOLTIP.toString()));
		totalRegions.setTooltip(new Tooltip(Translation.STATUS_TOTAL_TOOLTIP.toString()));
		grid.add(hoveredBlock, 0, 0, 1, 1);
		grid.add(hoveredChunk, 1, 0, 1, 1);
		grid.add(hoveredRegion, 2, 0, 1 ,1);
		grid.add(selectedChunks, 3, 0, 1, 1);
		grid.add(visibleRegions, 4, 0, 1, 1);
		grid.add(totalRegions, 5, 0, 1, 1);
		grid.add(overlay, 6, 0, 1, 1);

		StackPane.setAlignment(grid, Pos.CENTER_LEFT);
		getChildren().add(grid);

		rt = new RotateTransition(Duration.millis(1000), loadIcon);
		rt.setByAngle(360);
		rt.setCycleCount(Animation.INDEFINITE);
		rt.setInterpolator(Interpolator.LINEAR);

		StackPane.setAlignment(bp, Pos.CENTER_LEFT);
		getChildren().add(bp);

		DataProperty<Boolean> b = new DataProperty<>(true);
		DataProperty<Integer> before = new DataProperty<>(0);
		Thread t = new Thread(() -> {
			while (b.get()) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException ex) {
					return;
				}
				int activeJobs = MCAFilePipe.getActiveJobs();
				if (before.get() == 0 && activeJobs != 0) {
					Platform.runLater(() -> {
						rt.play();
						bp.setRight(loadIcon);
					});
				} else if (before.get() != 0 && activeJobs == 0) {
					Platform.runLater(() -> {
						rt.stop();
						bp.setRight(null);
					});
				}
				before.set(activeJobs);
			}
		});
		ShutdownHooks.addShutdownHook(() -> {
			b.set(false);
			t.interrupt();
		});
		t.start();
	}

	private void update(TileMap tileMap) {
		selectedChunks.setText(Translation.STATUS_SELECTED + ": " + (tileMap.isSelectionInverted() ? "?" : tileMap.getSelectedChunks()));
		visibleRegions.setText(Translation.STATUS_VISIBLE + ": " + tileMap.getVisibleTiles());
		totalRegions.setText(Translation.STATUS_TOTAL + ": " + tileMap.getLoadedTiles());
		Point2i b = tileMap.getHoveredBlock();
		if (b != null) {
			hoveredBlock.setText(Translation.STATUS_BLOCK + ": " + b.getX() + ", " + b.getZ());
			Point2i c = b.blockToChunk();
			hoveredChunk.setText(Translation.STATUS_CHUNK + ": " + c.getX() + ", " + c.getZ());
			Point2i r = b.blockToRegion();
			hoveredRegion.setText(Translation.STATUS_REGION + ": " + r.getX() + ", " + r.getZ());
			updateOverlay(tileMap, c);
		} else {
			hoveredBlock.setText(Translation.STATUS_BLOCK + ": -, -");
			hoveredChunk.setText(Translation.STATUS_CHUNK + ": -, -");
			hoveredRegion.setText(Translation.STATUS_REGION + ": -, -");
			updateOverlay(tileMap, null);
		}
	}

	private void updateOverlay(TileMap tileMap, Point2i chunk) {
		if (tileMap.getOverlay() != null) {
			OverlayParser p = tileMap.getOverlay();
			String s = p.getShortMultiValues();
			if (!p.isActive()) {
				overlay.setText(Translation.STATUS_OVERLAY + ": " + p.getType() + "(" + p.min() + ", " + p.max() + (s == null ? "" : ", " + s) + "), -");
			} else if (chunk != null) {
				tileMap.getOverlayPool().getHoveredChunkValue(chunk, v -> {
					overlay.setText(Translation.STATUS_OVERLAY + ": " + p.getType() + "(" + p.min() + ", " + p.max() + (s == null ? "" : ", " + s) + "), " + (v == null ? "-" : v));
				});
			} else {
				overlay.setText(Translation.STATUS_OVERLAY + ": " + p.getType() + "(" + p.min() + ", " + p.max() + (s == null ? "" : ", " + s) + "), -");
			}
		} else {
			overlay.setText(Translation.STATUS_OVERLAY + ": -, -");
		}
	}
}
