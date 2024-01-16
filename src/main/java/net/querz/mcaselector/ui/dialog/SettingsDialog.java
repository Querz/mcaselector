package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.config.GlobalConfig;
import net.querz.mcaselector.config.WorldConfig;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.component.BorderedTitledPane;
import net.querz.mcaselector.ui.component.FileTextField;
import net.querz.mcaselector.ui.component.HeightSlider;
import net.querz.mcaselector.ui.component.TileMapBox;
import net.querz.mcaselector.ui.UIFactory;
import java.io.File;
import java.util.*;

public class SettingsDialog extends Dialog<SettingsDialog.Result> {

	private static final int processorCount = Runtime.getRuntime().availableProcessors();
	private static final long maxMemory = Runtime.getRuntime().maxMemory();

	private final TabPane tabPane = new TabPane();
	private final ToggleGroup toggleGroup = new ToggleGroup();

	// use a custom box containing a group of ToggleButtons to be able to freely align the tabs
	private final BorderPane tabBox = new BorderPane();

	private final ComboBox<Locale> languages = new ComboBox<>();

	private final Slider processThreadsSlider = createSlider(1, processorCount * 2, 1, ConfigProvider.GLOBAL.getProcessThreads());
	private final Slider writeThreadsSlider = createSlider(1, processorCount, 1, ConfigProvider.GLOBAL.getWriteThreads());
	private final Slider maxLoadedFilesSlider = createSlider(1, (int) Math.max(Math.ceil(maxMemory / 1_000_000_000D) * 6, 4), 1, ConfigProvider.GLOBAL.getMaxLoadedFiles());
	private final HeightSlider hSlider = new HeightSlider(ConfigProvider.WORLD.getRenderHeight(), false);
	private final CheckBox layerOnly = new CheckBox();
	private final CheckBox caves = new CheckBox();
	private final Button regionSelectionColorPreview = new Button();
	private final Button chunkSelectionColorPreview = new Button();
	private final Button pasteChunksColorPreview = new Button();
	private final CheckBox shadeCheckBox = new CheckBox();
	private final CheckBox shadeWaterCheckBox = new CheckBox();
	private final CheckBox showNonexistentRegionsCheckBox = new CheckBox();
	private final CheckBox smoothRendering = new CheckBox();
	private final CheckBox smoothOverlays = new CheckBox();
	private final ComboBox<TileMapBox.TileMapBoxBackground> tileMapBackgrounds = new ComboBox<>();
	private final FileTextField mcSavesDir = new FileTextField();
	private final CheckBox debugCheckBox = new CheckBox();
	private final FileTextField poiField = new FileTextField();
	private final FileTextField entitiesField = new FileTextField();

	private Color regionSelectionColor = ConfigProvider.GLOBAL.getRegionSelectionColor().makeJavaFXColor();
	private Color chunkSelectionColor = ConfigProvider.GLOBAL.getChunkSelectionColor().makeJavaFXColor();
	private Color pasteChunksColor = ConfigProvider.GLOBAL.getPasteChunksColor().makeJavaFXColor();

	private final ButtonType reset = new ButtonType(Translation.DIALOG_SETTINGS_RESET.toString(), ButtonBar.ButtonData.LEFT);

	public SettingsDialog(Stage primaryStage, boolean renderSettings) {
		titleProperty().bind(Translation.DIALOG_SETTINGS_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("settings-dialog-pane");
		getDialogPane().getScene().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL, reset);

		getDialogPane().lookupButton(reset).addEventFilter(ActionEvent.ACTION, e -> {
			e.consume();
			languages.setValue(GlobalConfig.DEFAULT_LOCALE);
			processThreadsSlider.setValue(GlobalConfig.DEFAULT_PROCESS_THREADS);
			writeThreadsSlider.setValue(GlobalConfig.DEFAULT_WRITE_THREADS);
			maxLoadedFilesSlider.setValue(GlobalConfig.DEFAULT_MAX_LOADED_FILES);
			regionSelectionColor = GlobalConfig.DEFAULT_REGION_SELECTION_COLOR.makeJavaFXColor();
			regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(GlobalConfig.DEFAULT_REGION_SELECTION_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			chunkSelectionColor = GlobalConfig.DEFAULT_CHUNK_SELECTION_COLOR.makeJavaFXColor();
			chunkSelectionColorPreview.setBackground(new Background(new BackgroundFill(GlobalConfig.DEFAULT_CHUNK_SELECTION_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			pasteChunksColor = GlobalConfig.DEFAULT_PASTE_CHUNKS_COLOR.makeJavaFXColor();
			pasteChunksColorPreview.setBackground(new Background(new BackgroundFill(GlobalConfig.DEFAULT_PASTE_CHUNKS_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			shadeCheckBox.setSelected(WorldConfig.DEFAULT_SHADE);
			shadeWaterCheckBox.setSelected(WorldConfig.DEFAULT_SHADE_WATER);
			showNonexistentRegionsCheckBox.setSelected(WorldConfig.DEFAULT_SHOW_NONEXISTENT_REGIONS);
			smoothRendering.setSelected(WorldConfig.DEFAULT_SMOOTH_RENDERING);
			smoothOverlays.setSelected(WorldConfig.DEFAULT_SMOOTH_OVERLAYS);
			hSlider.valueProperty().set(hSlider.getValue());
			caves.setSelected(WorldConfig.DEFAULT_RENDER_CAVES);
			tileMapBackgrounds.setValue(TileMapBox.TileMapBoxBackground.valueOf(WorldConfig.DEFAULT_TILEMAP_BACKGROUND));
			mcSavesDir.setFile(GlobalConfig.DEFAULT_MC_SAVES_DIR == null ? null : new File(GlobalConfig.DEFAULT_MC_SAVES_DIR));
			debugCheckBox.setSelected(GlobalConfig.DEFAULT_DEBUG);
		});

		languages.getItems().addAll(Translation.getAvailableLanguages());
		languages.setValue(ConfigProvider.GLOBAL.getLocale());
		languages.setConverter(new StringConverter<>() {

			final Map<String, Locale> cache = new HashMap<>();

			@Override
			public String toString(Locale locale) {
				String display = locale.getDisplayName(locale);
				cache.put(display, locale);
				return display;
			}

			@Override
			public Locale fromString(String string) {
				return cache.get(string);
			}
		});
		languages.getStyleClass().add("languages-combo-box");

		regionSelectionColorPreview.getStyleClass().clear();
		chunkSelectionColorPreview.getStyleClass().clear();
		pasteChunksColorPreview.getStyleClass().clear();
		regionSelectionColorPreview.getStyleClass().add("color-preview-button");
		chunkSelectionColorPreview.getStyleClass().add("color-preview-button");
		pasteChunksColorPreview.getStyleClass().add("color-preview-button");
		regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(regionSelectionColor, CornerRadii.EMPTY, Insets.EMPTY)));
		chunkSelectionColorPreview.setBackground(new Background(new BackgroundFill(chunkSelectionColor, CornerRadii.EMPTY, Insets.EMPTY)));
		pasteChunksColorPreview.setBackground(new Background(new BackgroundFill(pasteChunksColor, CornerRadii.EMPTY, Insets.EMPTY)));
		shadeCheckBox.setSelected(ConfigProvider.WORLD.getShade());
		shadeWaterCheckBox.setSelected(ConfigProvider.WORLD.getShadeWater());
		showNonexistentRegionsCheckBox.setSelected(ConfigProvider.WORLD.getShowNonexistentRegions());
		smoothRendering.setSelected(ConfigProvider.WORLD.getSmoothRendering());
		smoothOverlays.setSelected(ConfigProvider.WORLD.getSmoothOverlays());
		hSlider.valueProperty().set(ConfigProvider.WORLD.getRenderHeight());
		layerOnly.setSelected(ConfigProvider.WORLD.getRenderLayerOnly());
		caves.setSelected(ConfigProvider.WORLD.getRenderCaves());
		tileMapBackgrounds.getItems().addAll(TileMapBox.TileMapBoxBackground.values());

		tileMapBackgrounds.setCellFactory((listView) -> {
			ListCell<TileMapBox.TileMapBoxBackground> cell = new ListCell<>() {

				@Override
				public void updateIndex(int i) {
					super.updateIndex(i);
					TileMapBox.TileMapBoxBackground[] values = TileMapBox.TileMapBoxBackground.values();
					if (i < 0 || i >= values.length) {
						return;
					}
					setBackground(values[i].getBackground());
				}
			};
			// we don't want this to be treated like a regular list cell
			cell.getStyleClass().clear();
			return cell;
		});
		tileMapBackgrounds.setButtonCell(tileMapBackgrounds.getCellFactory().call(null));
		tileMapBackgrounds.getStyleClass().add("tilemap-backgrounds-combo-box");

		tileMapBackgrounds.setValue(TileMapBox.TileMapBoxBackground.valueOf(ConfigProvider.WORLD.getTileMapBackground()));
		mcSavesDir.setFile(ConfigProvider.GLOBAL.getMcSavesDir() == null ? null : new File(ConfigProvider.GLOBAL.getMcSavesDir()));
		debugCheckBox.setSelected(ConfigProvider.GLOBAL.getDebug());

		regionSelectionColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), regionSelectionColor).showColorPicker();
			result.ifPresent(c -> {
				regionSelectionColor = c;
				regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
			});
		});
		chunkSelectionColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), chunkSelectionColor).showColorPicker();
			result.ifPresent(c -> {
				chunkSelectionColor = c;
				chunkSelectionColorPreview.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
			});
		});
		pasteChunksColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), pasteChunksColor).showColorPicker();
			result.ifPresent(c -> {
				pasteChunksColor = c;
				pasteChunksColorPreview.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
			});
		});

		shadeCheckBox.setOnAction(e -> shadeWaterCheckBox.setDisable(!shadeCheckBox.isSelected()));
		shadeWaterCheckBox.setDisable(!shadeCheckBox.isSelected() || caves.isSelected() || layerOnly.isSelected());
		shadeCheckBox.setDisable(caves.isSelected() || layerOnly.isSelected());

		layerOnly.setOnAction(e -> caves.setDisable(layerOnly.isSelected()));
		caves.setDisable(layerOnly.isSelected());
		layerOnly.setDisable(caves.isSelected());
		caves.setOnAction(e -> {
			layerOnly.setDisable(caves.isSelected());
			shadeCheckBox.setDisable(caves.isSelected());
			shadeWaterCheckBox.setDisable(!shadeCheckBox.isSelected() || caves.isSelected());
		});
		layerOnly.setOnAction(e -> {
			caves.setDisable(layerOnly.isSelected());
			shadeCheckBox.setDisable(layerOnly.isSelected());
			shadeWaterCheckBox.setDisable(!shadeCheckBox.isSelected() || layerOnly.isSelected());
		});

		HBox debugBox = new HBox();
		debugBox.getStyleClass().add("debug-box");
		Hyperlink logFileLink = UIFactory.explorerLink(Translation.DIALOG_SETTINGS_GLOBAL_MISC_SHOW_LOG_FILE, Config.BASE_LOG_DIR, null);
		debugBox.getChildren().addAll(debugCheckBox, logFileLink);

		if (ConfigProvider.WORLD.getWorldDirs() != null) {
			WorldDirectories worldDirectories = ConfigProvider.WORLD.getWorldDirs().clone();
			poiField.setFile(worldDirectories.getPoi());
			entitiesField.setFile(worldDirectories.getEntities());
		}

		hSlider.setMajorTickUnit(64);
		hSlider.setAlignment(Pos.CENTER_LEFT);

		toggleGroup.selectedToggleProperty().addListener((v, o, n) -> {
			if (n == null) {
				toggleGroup.selectToggle(o);
			} else {
				tabPane.getSelectionModel().select((Tab) n.getUserData());
			}
		});

		HBox leftTabs = new HBox();
		leftTabs.getStyleClass().add("tab-box");
		HBox rightTabs = new HBox();
		rightTabs.getStyleClass().add("tab-box");

		// -------------------------------------------------------------------------------------------------------------

		// GLOBAL
		Tab globalTab = UIFactory.tab(Translation.DIALOG_SETTINGS_TAB_GLOBAL);
		VBox globalBox = new VBox();

		GridPane languageGrid = createGrid();
		addPairToGrid(languageGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_GLOBAL_LANGUAGE_LANGUAGE), languages);
		BorderedTitledPane lang = new BorderedTitledPane(Translation.DIALOG_SETTINGS_GLOBAL_LANGUAGE, languageGrid);

		GridPane selectionsGrid = createGrid();
		addPairToGrid(selectionsGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_GLOBAL_SELECTION_REGION_COLOR), regionSelectionColorPreview);
		addPairToGrid(selectionsGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_GLOBAL_SELECTION_CHUNK_COLOR), chunkSelectionColorPreview);
		addPairToGrid(selectionsGrid, 2, UIFactory.label(Translation.DIALOG_SETTINGS_GLOBAL_SELECTION_PASTED_CHUNKS_COLOR), pasteChunksColorPreview);
		BorderedTitledPane selections = new BorderedTitledPane(Translation.DIALOG_SETTINGS_GLOBAL_SELECTION, selectionsGrid);

		GridPane miscGrid = createGrid();
		addPairToGrid(miscGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_GLOBAL_MISC_MC_SAVES_DIR), mcSavesDir);
		addPairToGrid(miscGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_GLOBAL_MISC_PRINT_DEBUG), debugBox);
		BorderedTitledPane misc = new BorderedTitledPane(Translation.DIALOG_SETTINGS_GLOBAL_MISC, miscGrid);

		globalBox.getChildren().addAll(lang, selections, misc);
		globalTab.setContent(globalBox);
		ToggleButton globalToggleButton = createToggleButton(globalTab, Translation.DIALOG_SETTINGS_TAB_GLOBAL);
		leftTabs.getChildren().add(globalToggleButton);

		// PROCESSING
		Tab processingTab = UIFactory.tab(Translation.DIALOG_SETTINGS_TAB_PROCESSING);
		VBox processingBox = new VBox();

		GridPane threadGrid = createGrid();
		addPairToGrid(threadGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_PROCESSING_PROCESS_PROCESS_THREADS), processThreadsSlider, UIFactory.attachTextFieldToSlider(processThreadsSlider));
		addPairToGrid(threadGrid, 2, UIFactory.label(Translation.DIALOG_SETTINGS_PROCESSING_PROCESS_WRITE_THREADS), writeThreadsSlider, UIFactory.attachTextFieldToSlider(writeThreadsSlider));
		BorderedTitledPane threads = new BorderedTitledPane(Translation.DIALOG_SETTINGS_PROCESSING_PROCESS, threadGrid);

		GridPane filesGrid = createGrid();
		addPairToGrid(filesGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_PROCESSING_FILES_MAX_FILES), maxLoadedFilesSlider, UIFactory.attachTextFieldToSlider(maxLoadedFilesSlider));
		BorderedTitledPane files = new BorderedTitledPane(Translation.DIALOG_SETTINGS_PROCESSING_FILES, filesGrid);

		processingBox.getChildren().addAll(threads, files);
		processingTab.setContent(processingBox);
		leftTabs.getChildren().add(createToggleButton(processingTab, Translation.DIALOG_SETTINGS_TAB_PROCESSING));

		// RENDERING
		Tab renderingTab = UIFactory.tab(Translation.DIALOG_SETTINGS_TAB_RENDERING);
		VBox renderingBox = new VBox();

		HBox shadingAndSmooth = new HBox();

		GridPane shadingGrid = createGrid();
		addPairToGrid(shadingGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SHADE_SHADE), shadeCheckBox);
		addPairToGrid(shadingGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SHADE_SHADE_WATER), shadeWaterCheckBox);
		BorderedTitledPane shade = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_SHADE, shadingGrid);

		GridPane smoothGrid = createGrid();
		addPairToGrid(smoothGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SMOOTH_SMOOTH_RENDERING), smoothRendering);
		addPairToGrid(smoothGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SMOOTH_SMOOTH_OVERLAYS), smoothOverlays);
		BorderedTitledPane smooth = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_SMOOTH, smoothGrid);

		HBox.setHgrow(shade, Priority.ALWAYS);
		HBox.setHgrow(smooth, Priority.ALWAYS);
		shadingAndSmooth.getChildren().addAll(shade, smooth);

		GridPane layerGrid = createGrid();
		addPairToGrid(layerGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_HEIGHT), hSlider);
		addPairToGrid(layerGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_LAYER_ONLY), layerOnly);
		addPairToGrid(layerGrid, 2, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_CAVES), caves);
		BorderedTitledPane layers = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_LAYERS, layerGrid);

		GridPane backgroundGrid = createGrid();
		addPairToGrid(backgroundGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_BACKGROUND_BACKGROUND_PATTERN), tileMapBackgrounds);
		addPairToGrid(backgroundGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_BACKGROUND_SHOW_NONEXISTENT_REGIONS), showNonexistentRegionsCheckBox);
		BorderedTitledPane background = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_BACKGROUND, backgroundGrid);

		renderingBox.getChildren().addAll(shadingAndSmooth, layers, background);
		renderingTab.setContent(renderingBox);
		ToggleButton renderingToggleButton = createToggleButton(renderingTab, Translation.DIALOG_SETTINGS_TAB_RENDERING);
		rightTabs.getChildren().add(renderingToggleButton);

		// WORLD
		Tab worldTab = UIFactory.tab(Translation.DIALOG_SETTINGS_TAB_WORLD);
		VBox worldBox = new VBox();

		GridPane worldGrid = createGrid();
		addPairToGrid(worldGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_WORLD_PATHS_POI), poiField);
		addPairToGrid(worldGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_WORLD_PATHS_ENTITIES), entitiesField);
		BorderedTitledPane world = new BorderedTitledPane(Translation.DIALOG_SETTINGS_WORLD_PATHS, worldGrid);

		worldBox.getChildren().addAll(world);
		worldTab.setContent(worldBox);
		ToggleButton worldToggleButton = createToggleButton(worldTab, Translation.DIALOG_SETTINGS_TAB_WORLD);
		rightTabs.getChildren().add(worldToggleButton);

		// -------------------------------------------------------------------------------------------------------------

		renderingTab.setDisable(ConfigProvider.WORLD.getWorldDirs() == null);
		worldTab.setDisable(ConfigProvider.WORLD.getWorldDirs() == null);
		renderingToggleButton.setDisable(ConfigProvider.WORLD.getWorldDirs() == null);
		worldToggleButton.setDisable(ConfigProvider.WORLD.getWorldDirs() == null);

		tabPane.getTabs().addAll(globalTab, processingTab, renderingTab, worldTab);

		final DataProperty<Tab> focusedTab = new DataProperty<>(globalTab);
		if (ConfigProvider.WORLD.getWorldDirs() != null && renderSettings) {
			focusedTab.set(renderingTab);
			toggleGroup.selectToggle(renderingToggleButton);
		} else {
			toggleGroup.selectToggle(globalToggleButton);
		}

		Platform.runLater(() -> focusedTab.get().getContent().requestFocus());

		tabBox.setLeft(leftTabs);
		tabBox.setRight(rightTabs);

		VBox content = new VBox();
		content.getChildren().addAll(tabBox, tabPane);

		getDialogPane().setContent(content);

		setResultConverter(c -> {
			if (c.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
				return new Result(
					languages.getSelectionModel().getSelectedItem(),
					(int) processThreadsSlider.getValue(),
					(int) writeThreadsSlider.getValue(),
					(int) maxLoadedFilesSlider.getValue(),
					regionSelectionColor,
					chunkSelectionColor,
					pasteChunksColor,
					shadeCheckBox.isSelected(),
					shadeWaterCheckBox.isSelected(),
					showNonexistentRegionsCheckBox.isSelected(),
					smoothRendering.isSelected(),
					smoothOverlays.isSelected(),
					tileMapBackgrounds.getSelectionModel().getSelectedItem(),
					mcSavesDir.getFile(),
					debugCheckBox.isSelected(),
					hSlider.getValue(),
					layerOnly.isSelected(),
					caves.isSelected(),
					poiField.getFile(),
					entitiesField.getFile());
			}
			return null;
		});

		getDialogPane().getStylesheets().add(SettingsDialog.class.getClassLoader().getResource("style/component/settings-dialog.css").toExternalForm());
	}

	private <T extends Node> T withAlignment(T node) {
		GridPane.setFillWidth(node, true);
		return node;
	}

	private ToggleButton createToggleButton(Tab tab, Translation name) {
		ToggleButton toggleButton = new ToggleButton();
		toggleButton.textProperty().bind(name.getProperty());
		toggleButton.setToggleGroup(toggleGroup);
		toggleButton.setUserData(tab);
		return toggleButton;
	}

	private GridPane createGrid() {
		GridPane grid = new GridPane();
		grid.getStyleClass().add("slider-grid-pane");
		return grid;
	}

	private void addPairToGrid(GridPane grid, int y, Label key, Node... value) {
		if (value.length == 0 || value.length > 2) {
			throw new IllegalArgumentException("invalid number of arguments (" + value.length + ") for addPairToGrid");
		}
		grid.add(key, 0, y, 1, 1);
		for (int i = 0; i < value.length; i++) {
			grid.add(withAlignment(value[i]), i + 1, y, value.length == 1 ? 2 : 1, 1);
		}
	}

	private Slider createSlider(int min, int max, int steps, int init) {
		if (max < min) {
			max = min;
		}
		Slider slider = new Slider(min, max, init);
		int majorTicks = Math.max((int) Math.ceil(max - min) / 5, 1);
		slider.setMajorTickUnit(majorTicks);
		slider.setMinorTickCount(majorTicks - 1);
		slider.setBlockIncrement(steps);
		return slider;
	}

	public static class Result {

		public final int processThreads, writeThreads, maxLoadedFiles;
		public final Color regionColor, chunkColor, pasteColor;
		public final boolean shadeWater;
		public final boolean shade;
		public final boolean showNonexistentRegions;
		public final boolean smoothRendering, smoothOverlays;
		public final TileMapBox.TileMapBoxBackground tileMapBackground;
		public final File mcSavesDir;
		public final boolean debug;
		public final Locale locale;
		public final int height;
		public final boolean layerOnly, caves;
		public final File poi, entities;

		public Result(Locale locale, int processThreads, int writeThreads, int maxLoadedFiles,
		              Color regionColor, Color chunkColor, Color pasteColor, boolean shade, boolean shadeWater,
		              boolean showNonexistentRegions, boolean smoothRendering, boolean smoothOverlays,
		              TileMapBox.TileMapBoxBackground tileMapBackground, File mcSavesDir, boolean debug, int height,
		              boolean layerOnly, boolean caves, File poi, File entities) {

			this.locale = locale;
			this.processThreads = processThreads;
			this.writeThreads = writeThreads;
			this.maxLoadedFiles = maxLoadedFiles;
			this.regionColor = regionColor;
			this.chunkColor = chunkColor;
			this.pasteColor = pasteColor;
			this.shade = shade;
			this.shadeWater = shadeWater;
			this.showNonexistentRegions = showNonexistentRegions;
			this.smoothRendering = smoothRendering;
			this.smoothOverlays = smoothOverlays;
			this.tileMapBackground = tileMapBackground;
			this.mcSavesDir = Objects.requireNonNullElseGet(mcSavesDir, () -> new File(GlobalConfig.DEFAULT_MC_SAVES_DIR));
			this.debug = debug;
			this.height = height;
			this.layerOnly = layerOnly;
			this.caves = caves;
			this.poi = poi;
			this.entities = entities;
		}
	}
}
