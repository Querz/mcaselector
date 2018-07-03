package net.querz.mcaselector.tiles;

import javafx.beans.value.ChangeListener;
import javafx.css.*;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.querz.mcaselector.util.Debug;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TileMapBox extends HBox {

	private final StyleableObjectProperty<Color> markedRegionColorProperty = new SimpleStyleableObjectProperty<>(markedRegionColorMetaData, this, "markedRegionColor");
	private static final CssMetaData<TileMapBox, Color> markedRegionColorMetaData = new CssMetaData<TileMapBox, Color>("-marked-region-color", StyleConverter.getColorConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.markedRegionColorProperty.isBound();
		}

		@Override
		public StyleableProperty<Color> getStyleableProperty(TileMapBox styleable) {
			return styleable.markedRegionColorProperty;
		}
	};

	private final StyleableObjectProperty<Color> markedChunkColorProperty = new SimpleStyleableObjectProperty<>(markedRegionColorMetaData, this, "markedChunkColor");
	private static final CssMetaData<TileMapBox, Color> markedChunkColorMetaData = new CssMetaData<TileMapBox, Color>("-marked-chunk-color", StyleConverter.getColorConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.markedChunkColorProperty.isBound();
		}

		@Override
		public StyleableProperty<Color> getStyleableProperty(TileMapBox styleable) {
			return styleable.markedChunkColorProperty;
		}
	};

	private final StyleableObjectProperty<Color> regionGridColorProperty = new SimpleStyleableObjectProperty<>(regionGridColorMetaData, this, "regionGridColor");
	private static final CssMetaData<TileMapBox, Color> regionGridColorMetaData = new CssMetaData<TileMapBox, Color>("-region-grid-color", StyleConverter.getColorConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.regionGridColorProperty.isBound();
		}

		@Override
		public StyleableProperty<Color> getStyleableProperty(TileMapBox styleable) {
			return styleable.regionGridColorProperty;
		}
	};

	private final StyleableObjectProperty<Color> chunkGridColorProperty = new SimpleStyleableObjectProperty<>(chunkGridColorMetaData, this, "chunkGridColor");
	private static final CssMetaData<TileMapBox, Color> chunkGridColorMetaData = new CssMetaData<TileMapBox, Color>("-chunk-grid-color", StyleConverter.getColorConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.chunkGridColorProperty.isBound();
		}

		@Override
		public StyleableProperty<Color> getStyleableProperty(TileMapBox styleable) {
			return styleable.chunkGridColorProperty;
		}
	};

	private final StyleableDoubleProperty gridLineWidthProperty = new SimpleStyleableDoubleProperty(gridLineWidthMetaData, this, "gridLineWidth");
	private static final CssMetaData<TileMapBox, Number> gridLineWidthMetaData = new CssMetaData<TileMapBox, Number>("-grid-line-width", StyleConverter.getSizeConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.gridLineWidthProperty.isBound();
		}

		@Override
		public StyleableProperty<Number> getStyleableProperty(TileMapBox styleable) {
			return styleable.gridLineWidthProperty;
		}
	};

	private final StyleableObjectProperty<Color> emptyChunkBackgroundColorProperty = new SimpleStyleableObjectProperty<>(emptyChunkBackgroundColorMetaData, this, "emptyChunkBackgroundColor");
	private static final CssMetaData<TileMapBox, Color> emptyChunkBackgroundColorMetaData = new CssMetaData<TileMapBox, Color>("-empty-chunk-background-color", StyleConverter.getColorConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.emptyChunkBackgroundColorProperty.isBound();
		}

		@Override
		public StyleableProperty<Color> getStyleableProperty(TileMapBox styleable) {
			return styleable.emptyChunkBackgroundColorProperty;
		}
	};

	private final StyleableObjectProperty<Color> emptyColorProperty = new SimpleStyleableObjectProperty<>(emptyColorMetaData, this, "emptyColor");
	private static final CssMetaData<TileMapBox, Color> emptyColorMetaData = new CssMetaData<TileMapBox, Color>("-empty-color", StyleConverter.getColorConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.emptyColorProperty.isBound();
		}

		@Override
		public StyleableProperty<Color> getStyleableProperty(TileMapBox styleable) {
			return styleable.emptyColorProperty;
		}
	};

	private static final List<CssMetaData<? extends Styleable, ?>> CLASS_CSS_META_DATA;

	static {
		// combine already available properties in HBox with new properties
		List<CssMetaData<? extends Styleable, ?>> parent = HBox.getClassCssMetaData();
		List<CssMetaData<? extends Styleable, ?>> additional = Arrays.asList(
				markedRegionColorMetaData,
				markedChunkColorMetaData,
				regionGridColorMetaData,
				chunkGridColorMetaData,
				gridLineWidthMetaData,
				emptyChunkBackgroundColorMetaData,
				emptyColorMetaData
		);
		List<CssMetaData<? extends Styleable, ?>> own = new ArrayList<>(parent.size() + additional.size());
		own.addAll(parent);
		own.addAll(additional);
		CLASS_CSS_META_DATA = Collections.unmodifiableList(own);
	}

	public TileMapBox(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("tile-map-box");
		ChangeListener<Number> sizeListener = (o, r, n) -> {
			tileMap.resize(primaryStage.getWidth(), primaryStage.getHeight());
			Debug.dump("resizing to " + primaryStage.getWidth() + " " + primaryStage.getHeight());
		};
		primaryStage.widthProperty().addListener(sizeListener);
		primaryStage.heightProperty().addListener(sizeListener);
		setAlignment(Pos.TOP_LEFT);
		getChildren().add(tileMap);
		bind();
	}

	private void bind() {
		markedRegionColorProperty.addListener((o, r, n) -> Tile.REGION_MARKED_COLOR = markedRegionColorProperty.get());
		markedChunkColorProperty.addListener((o, r, n) -> Tile.CHUNK_MARKED_COLOR = markedChunkColorProperty.get());
		regionGridColorProperty.addListener((o, r, n) -> Tile.REGION_GRID_COLOR = regionGridColorProperty.get());
		chunkGridColorProperty.addListener((o, r, n) -> Tile.CHUNK_GRID_COLOR = chunkGridColorProperty.get());
		gridLineWidthProperty.addListener((o, r, n) -> Tile.GRID_LINE_WIDTH = gridLineWidthProperty.get());
		emptyChunkBackgroundColorProperty.addListener((o, r, n) -> Tile.EMPTY_CHUNK_BACKGROUND_COLOR = emptyChunkBackgroundColorProperty.get());
		emptyColorProperty.addListener((o, r, n) -> {
			Tile.EMPTY_COLOR = emptyColorProperty.get();
			Tile.reloadEmpty();
		});
	}

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
		return CLASS_CSS_META_DATA;
	}
}
