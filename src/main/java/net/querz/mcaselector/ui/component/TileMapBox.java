package net.querz.mcaselector.ui.component;

import javafx.beans.value.ChangeListener;
import javafx.css.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.config.WorldConfig;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.tile.TileMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TileMapBox extends HBox {

	private final StyleableObjectProperty<Color> regionGridColorProperty = new SimpleStyleableObjectProperty<>(regionGridColorMetaData, this, "regionGridColor");
	private static final CssMetaData<TileMapBox, Color> regionGridColorMetaData = new CssMetaData<>("-region-grid-color", StyleConverter.getColorConverter()) {
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
	private static final CssMetaData<TileMapBox, Color> chunkGridColorMetaData = new CssMetaData<>("-chunk-grid-color", StyleConverter.getColorConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.chunkGridColorProperty.isBound();
		}

		@Override
		public StyleableProperty<Color> getStyleableProperty(TileMapBox styleable) {
			return styleable.chunkGridColorProperty;
		}
	};

	private final StyleableObjectProperty<Color> coordinatesColorProperty = new SimpleStyleableObjectProperty<>(coordinatesColorMetaData, this, "coordinatesColor");
	private static final CssMetaData<TileMapBox, Color> coordinatesColorMetaData = new CssMetaData<>("-coordinates-color", StyleConverter.getColorConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.coordinatesColorProperty.isBound();
		}

		@Override
		public StyleableProperty<Color> getStyleableProperty(TileMapBox styleable) {
			return styleable.coordinatesColorProperty;
		}
	};

	private final StyleableDoubleProperty gridLineWidthProperty = new SimpleStyleableDoubleProperty(gridLineWidthMetaData, this, "gridLineWidth");
	private static final CssMetaData<TileMapBox, Number> gridLineWidthMetaData = new CssMetaData<>("-grid-line-width", StyleConverter.getSizeConverter()) {
		@Override
		public boolean isSettable(TileMapBox styleable) {
			return !styleable.gridLineWidthProperty.isBound();
		}

		@Override
		public StyleableProperty<Number> getStyleableProperty(TileMapBox styleable) {
			return styleable.gridLineWidthProperty;
		}
	};

	private final StyleableObjectProperty<Color> emptyColorProperty = new SimpleStyleableObjectProperty<>(emptyColorMetaData, this, "emptyColor");
	private static final CssMetaData<TileMapBox, Color> emptyColorMetaData = new CssMetaData<>("-empty-color", StyleConverter.getColorConverter()) {
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
				regionGridColorMetaData,
				chunkGridColorMetaData,
				coordinatesColorMetaData,
				gridLineWidthMetaData,
				emptyColorMetaData
		);
		List<CssMetaData<? extends Styleable, ?>> own = new ArrayList<>(parent.size() + additional.size());
		own.addAll(parent);
		own.addAll(additional);
		CLASS_CSS_META_DATA = Collections.unmodifiableList(own);
	}

	public TileMapBox(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("tile-map-box");
		getStylesheets().add(TileMapBox.class.getClassLoader().getResource("style/component/tile-map-box.css").toExternalForm());
		ChangeListener<Number> sizeListener = (o, r, n) -> {
			tileMap.resize(primaryStage.getWidth(), primaryStage.getHeight());
		};
		primaryStage.widthProperty().addListener(sizeListener);
		primaryStage.heightProperty().addListener(sizeListener);
		setAlignment(Pos.TOP_LEFT);
		getChildren().add(tileMap);
		bind();

		try {
			setBackground(TileMapBoxBackground.valueOf(ConfigProvider.WORLD == null ? WorldConfig.DEFAULT_TILEMAP_BACKGROUND : ConfigProvider.WORLD.getTileMapBackground()).getBackground());
		} catch (IllegalArgumentException ex) {
			setBackground(TileMapBoxBackground.valueOf(WorldConfig.DEFAULT_TILEMAP_BACKGROUND).getBackground());
		}
	}

	private void bind() {
		regionGridColorProperty.addListener((o, r, n) -> Tile.REGION_GRID_COLOR = new net.querz.mcaselector.ui.Color(regionGridColorProperty.get()));
		chunkGridColorProperty.addListener((o, r, n) -> Tile.CHUNK_GRID_COLOR = new net.querz.mcaselector.ui.Color(chunkGridColorProperty.get()));
		coordinatesColorProperty.addListener((o, r, n) -> Tile.COORDINATES_COLOR = new net.querz.mcaselector.ui.Color(coordinatesColorProperty.get()));
		gridLineWidthProperty.addListener((o, r, n) -> Tile.GRID_LINE_WIDTH = gridLineWidthProperty.get());
		emptyColorProperty.addListener((o, r, n) -> {
			Tile.EMPTY_COLOR = new net.querz.mcaselector.ui.Color(emptyColorProperty.get());
			ImageHelper.reloadEmpty();
		});
	}

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
		return CLASS_CSS_META_DATA;
	}

	public enum TileMapBoxBackground {

		GREY_CHECKERBOARD(createBackgroundFromImage("img/background/grey_checkerboard")),
		PURPLE_CHECKERBOARD(createBackgroundFromImage("img/background/purple_checkerboard")),
		BLUE_CHECKERBOARD(createBackgroundFromImage("img/background/blue_checkerboard")),
		BLACK(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))),
		GREY(new Background(new BackgroundFill(Color.GREY, CornerRadii.EMPTY, Insets.EMPTY))),
		WHITE(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		private static Background createBackgroundFromImage(String resource) {
			return new Background(new BackgroundImage(FileHelper.getIconFromResources(resource),
				BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT,
				BackgroundSize.DEFAULT));
		}

		private final Background background;

		TileMapBoxBackground(Background background) {
			this.background = background;
		}

		public Background getBackground() {
			return background;
		}
	}
}
