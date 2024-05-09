package net.querz.mcaselector.ui.component;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.validation.ShutdownHooks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StatusBar extends StackPane {

	private static final int LOADING_ANIM_UPDATE_MILLIS = 500;

	private final GridPane grid = new GridPane();
	private final Label selectedChunks = new Label(Translation.STATUS_SELECTED + ": 0");
	private final Label hoveredRegion = new Label(Translation.STATUS_REGION + ": -, -");
	private final Label hoveredChunk = new Label(Translation.STATUS_CHUNK + ": -, -");
	private final Label hoveredBlock = new Label(Translation.STATUS_BLOCK + ": -, -");
	private final Label totalRegions = new Label(Translation.STATUS_TOTAL + ": 0");
	private final Label queuedJobs = new Label(Translation.STATUS_QUEUE + ": 0");
	private final Label overlay = new Label(Translation.STATUS_OVERLAY + ": -");

	ImageView loadIcon = new ImageView(FileHelper.getIconFromResources("img/load"));
	BorderPane bp = new BorderPane();
	RotateTransition rt;

	public StatusBar(TileMap tileMap) {
		getStyleClass().add("status-bar");
		getStylesheets().add(StatusBar.class.getClassLoader().getResource("style/component/status-bar.css").toExternalForm());
		grid.getStyleClass().add("status-bar-grid");

		tileMap.setOnUpdate(this::update);
		tileMap.setOnHover(this::update);
		for (int i = 0; i < 6; i++) {
			ColumnConstraints constraints = new ColumnConstraints();
			constraints.setMinWidth(140);
			constraints.setFillWidth(true);
			grid.getColumnConstraints().add(constraints);
		}
		grid.add(hoveredBlock, 0, 0, 1, 1);
		grid.add(hoveredChunk, 1, 0, 1, 1);
		grid.add(hoveredRegion, 2, 0, 1 ,1);
		grid.add(selectedChunks, 3, 0, 1, 1);
		grid.add(totalRegions, 4, 0, 1, 1);
		grid.add(queuedJobs, 5, 0, 1, 1);
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
		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		exec.scheduleAtFixedRate(() -> {
			if (!b.get())
				return;
			int activeJobs = JobHandler.getActiveJobs();
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
		}, LOADING_ANIM_UPDATE_MILLIS, LOADING_ANIM_UPDATE_MILLIS, TimeUnit.MILLISECONDS);
		ShutdownHooks.addShutdownHook(() -> {
			b.set(false);
			exec.shutdownNow();
		});
	}

	private void update(TileMap tileMap) {
		selectedChunks.setText(Translation.STATUS_SELECTED + ": " + (tileMap.getSelection().isInverted() ? "\u221e" : tileMap.getSelectedChunks()));
		queuedJobs.setText(Translation.STATUS_QUEUE + ": " + JobHandler.getActiveJobs());
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
			Overlay p = tileMap.getOverlay();
			String s = p.getShortMultiValues();
			if (!p.isActive()) {
				overlay.setText(Translation.STATUS_OVERLAY + ": " + p.getType() + "(" + p.min() + ", " + p.max() + (s == null ? "" : ", " + s) + "), -");
			} else if (chunk != null) {
				tileMap.getOverlayPool().getHoveredChunkValue(chunk, v -> overlay.setText(Translation.STATUS_OVERLAY + ": " + p.getType() + "(" + p.min() + ", " + p.max() + (s == null ? "" : ", " + s) + "), " + (v == null ? "-" : v)));
			} else {
				overlay.setText(Translation.STATUS_OVERLAY + ": " + p.getType() + "(" + p.min() + ", " + p.max() + (s == null ? "" : ", " + s) + "), -");
			}
		} else {
			overlay.setText(Translation.STATUS_OVERLAY + ": -, -");
		}
	}
}
