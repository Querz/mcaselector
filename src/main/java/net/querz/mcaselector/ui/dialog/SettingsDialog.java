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
import net.querz.mcaselector.ui.component.*;
import net.querz.mcaselector.util.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;
import java.io.File;
import java.util.*;

public class SettingsDialog extends Dialog<SettingsDialog.Result> {

	private static final int processorCount = Runtime.getRuntime().availableProcessors();
	private static final long maxMemory = Runtime.getRuntime().maxMemory();

	private final TabPane tabPane = new TabPane();
	private final ToggleGroup toggleGroup = new ToggleGroup();
	private final ComboBox<Locale> languages = new ComboBox<>();
	private final Slider processThreadsSlider;
	private final Slider writeThreadsSlider;
	private final Slider maxLoadedFilesSlider;
	private final HeightSlider hSlider;
	private final CheckBox layerOnly = new CheckBox();
	private final CheckBox caves = new CheckBox();
	private final Button regionSelectionColorPreview = new Button();
	private final Button chunkSelectionColorPreview = new Button();
	private final Button pasteChunksColorPreview = new Button();
	private final CheckBox shadeCheckBox = new CheckBox();
	private final CheckBox shadeWaterCheckBox = new CheckBox();
	private final CheckBox shadeAltitudeCheckBox = new CheckBox();
	private final CheckBox showNonexistentRegionsCheckBox = new CheckBox();
	private final ZoomLevelSlider zoomLevelSlider;
	private final Button zoomLevelLODColorPreview = new Button();
	private final CheckBox smoothRendering = new CheckBox();
	private final CheckBox smoothOverlays = new CheckBox();
	private final Slider structureIconSize;
	private final Slider structureIconBorderSize;
	private final ComboBox<TileMapBox.TileMapBoxBackground> tileMapBackgrounds = new ComboBox<>();
	private final FileTextField mcSavesDir = new FileTextField();
	private final CheckBox debugCheckBox = new CheckBox();
	private final FileTextField poiField = new FileTextField();
	private final FileTextField entitiesField = new FileTextField();

	public SettingsDialog(Stage primaryStage, boolean renderSettings) {
		setDialogPane(new CustomOrderDialogPane());
		titleProperty().bind(Translation.DIALOG_SETTINGS_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("settings-dialog-pane");
		getDialogPane().getScene().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		setResizable(true);
		ButtonType reset = new ButtonType(Translation.DIALOG_SETTINGS_RESET.toString(), ButtonBar.ButtonData.LEFT);
		ButtonType setDefault = new ButtonType(Translation.DIALOG_SETTINGS_SET_DEFAULT.toString(), ButtonBar.ButtonData.APPLY); // TODO: translation
		getDialogPane().getButtonTypes().addAll(reset, setDefault, ButtonType.OK, ButtonType.CANCEL);

		GlobalConfig tmpGlobalCfg = ConfigProvider.GLOBAL.copyForSettings();
		WorldConfig tmpWorldCfg = ConfigProvider.WORLD.copyForSettings();
		if (ConfigProvider.WORLD.getWorldDirs() != null) {
			tmpWorldCfg.setWorldDirs(ConfigProvider.WORLD.getWorldDirs().clone());
		}

		DataProperty<Color> regionSelectionColor = new DataProperty<>(tmpGlobalCfg.getRegionSelectionColor().makeJavaFXColor());
		DataProperty<Color> chunkSelectionColor = new DataProperty<>(tmpGlobalCfg.getChunkSelectionColor().makeJavaFXColor());
		DataProperty<Color> pasteChunksColor = new DataProperty<>(tmpGlobalCfg.getPasteChunksColor().makeJavaFXColor());
		DataProperty<Color> zoomLevelLODColor = new DataProperty<>(tmpWorldCfg.getZoomLevelLODColor().makeJavaFXColor());

		languages.getItems().addAll(Translation.getAvailableLanguages());
		languages.setValue(tmpGlobalCfg.getLocale());
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
		zoomLevelLODColorPreview.getStyleClass().clear();
		regionSelectionColorPreview.getStyleClass().add("color-preview-button");
		chunkSelectionColorPreview.getStyleClass().add("color-preview-button");
		pasteChunksColorPreview.getStyleClass().add("color-preview-button");
		zoomLevelLODColorPreview.getStyleClass().add("color-preview-button");
		regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(regionSelectionColor.get(), CornerRadii.EMPTY, Insets.EMPTY)));
		chunkSelectionColorPreview.setBackground(new Background(new BackgroundFill(chunkSelectionColor.get(), CornerRadii.EMPTY, Insets.EMPTY)));
		pasteChunksColorPreview.setBackground(new Background(new BackgroundFill(pasteChunksColor.get(), CornerRadii.EMPTY, Insets.EMPTY)));
		zoomLevelLODColorPreview.setBackground(new Background(new BackgroundFill(zoomLevelLODColor.get(), CornerRadii.EMPTY, Insets.EMPTY)));
		shadeCheckBox.setSelected(tmpWorldCfg.getShade());
		shadeWaterCheckBox.setSelected(tmpWorldCfg.getShadeWater());
		shadeAltitudeCheckBox.setSelected(tmpWorldCfg.getShadeAltitude());
		showNonexistentRegionsCheckBox.setSelected(tmpWorldCfg.getShowNonexistentRegions());
		smoothRendering.setSelected(tmpWorldCfg.getSmoothRendering());
		smoothOverlays.setSelected(tmpWorldCfg.getSmoothOverlays());
		layerOnly.setSelected(tmpWorldCfg.getRenderLayerOnly());
		caves.setSelected(tmpWorldCfg.getRenderCaves());
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

		tileMapBackgrounds.setValue(TileMapBox.TileMapBoxBackground.valueOf(tmpWorldCfg.getTileMapBackground()));
		mcSavesDir.setFile(tmpGlobalCfg.getMcSavesDir() == null ? null : new File(tmpGlobalCfg.getMcSavesDir()));
		debugCheckBox.setSelected(tmpGlobalCfg.getDebug());
		regionSelectionColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), regionSelectionColor.get()).showColorPicker();
			result.ifPresent(c -> {
				regionSelectionColor.set(c);
				regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
			});
		});
		chunkSelectionColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), chunkSelectionColor.get()).showColorPicker();
			result.ifPresent(c -> {
				chunkSelectionColor.set(c);
				chunkSelectionColorPreview.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
			});
		});
		pasteChunksColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), pasteChunksColor.get()).showColorPicker();
			result.ifPresent(c -> {
				pasteChunksColor.set(c);
				pasteChunksColorPreview.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
			});
		});

		shadeCheckBox.setOnAction(e -> {
			shadeWaterCheckBox.setDisable(!shadeCheckBox.isSelected());
			shadeAltitudeCheckBox.setDisable(!shadeCheckBox.isSelected());
		});
		shadeWaterCheckBox.setDisable(!shadeCheckBox.isSelected() || caves.isSelected() || layerOnly.isSelected());
		shadeAltitudeCheckBox.setDisable(!shadeCheckBox.isSelected() || caves.isSelected() || layerOnly.isSelected());
		shadeCheckBox.setDisable(caves.isSelected() || layerOnly.isSelected());

		layerOnly.setOnAction(e -> caves.setDisable(layerOnly.isSelected()));
		caves.setDisable(layerOnly.isSelected());
		layerOnly.setDisable(caves.isSelected());
		caves.setOnAction(e -> {
			layerOnly.setDisable(caves.isSelected());
			shadeCheckBox.setDisable(caves.isSelected());
			shadeWaterCheckBox.setDisable(!shadeCheckBox.isSelected() || caves.isSelected() || layerOnly.isSelected());
			shadeAltitudeCheckBox.setDisable(!shadeCheckBox.isSelected() || caves.isSelected() || layerOnly.isSelected());
		});
		layerOnly.setOnAction(e -> {
			caves.setDisable(layerOnly.isSelected());
			shadeCheckBox.setDisable(layerOnly.isSelected());
			shadeWaterCheckBox.setDisable(!shadeCheckBox.isSelected() || caves.isSelected() || layerOnly.isSelected());
			shadeAltitudeCheckBox.setDisable(!shadeCheckBox.isSelected() || caves.isSelected() || layerOnly.isSelected());
		});

		HBox debugBox = new HBox();
		debugBox.getStyleClass().add("debug-box");
		Hyperlink logFileLink = UIFactory.explorerLink(Translation.DIALOG_SETTINGS_GLOBAL_MISC_SHOW_LOG_FILE, Config.BASE_LOG_DIR, null);
		debugBox.getChildren().addAll(debugCheckBox, logFileLink);

		if (tmpWorldCfg.getWorldDirs() != null) {
			poiField.setFile(tmpWorldCfg.getWorldDirs().getPoi());
			entitiesField.setFile(tmpWorldCfg.getWorldDirs().getEntities());
		}

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
		processThreadsSlider = createSlider(1, processorCount * 2, 1, tmpGlobalCfg.getProcessThreads());
		writeThreadsSlider = createSlider(1, processorCount, 1, tmpGlobalCfg.getWriteThreads());
		addPairToGrid(threadGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_PROCESSING_PROCESS_PROCESS_THREADS), processThreadsSlider, UIFactory.attachTextFieldToSlider(processThreadsSlider));
		addPairToGrid(threadGrid, 2, UIFactory.label(Translation.DIALOG_SETTINGS_PROCESSING_PROCESS_WRITE_THREADS), writeThreadsSlider, UIFactory.attachTextFieldToSlider(writeThreadsSlider));
		BorderedTitledPane threads = new BorderedTitledPane(Translation.DIALOG_SETTINGS_PROCESSING_PROCESS, threadGrid);

		GridPane filesGrid = createGrid();
		maxLoadedFilesSlider = createSlider(1, (int) Math.max(Math.ceil(maxMemory / 1_000_000_000D) * 6, 4), 1, tmpGlobalCfg.getMaxLoadedFiles());
		addPairToGrid(filesGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_PROCESSING_FILES_MAX_FILES), maxLoadedFilesSlider, UIFactory.attachTextFieldToSlider(maxLoadedFilesSlider));
		BorderedTitledPane files = new BorderedTitledPane(Translation.DIALOG_SETTINGS_PROCESSING_FILES, filesGrid);

		processingBox.getChildren().addAll(threads, files);
		processingTab.setContent(processingBox);
		leftTabs.getChildren().add(createToggleButton(processingTab, Translation.DIALOG_SETTINGS_TAB_PROCESSING));

		// RENDERING
		Tab renderingTab = UIFactory.tab(Translation.DIALOG_SETTINGS_TAB_RENDERING);
		ScrollPane renderingScrollPane = new ScrollPane();
		renderingScrollPane.getStyleClass().clear();
		renderingScrollPane.getStyleClass().add("rendering-scroll-pane");
		renderingScrollPane.setFitToWidth(true);
		renderingScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		renderingScrollPane.setMinWidth(ScrollPane.USE_PREF_SIZE);
		VBox renderingBox = new VBox();

		HBox shadingAndSmooth = new HBox();

		GridPane shadingGrid = createGrid();
		addPairToGrid(shadingGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SHADE_SHADE), shadeCheckBox);
		addPairToGrid(shadingGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SHADE_SHADE_WATER), shadeWaterCheckBox);
		addPairToGrid(shadingGrid, 2, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SHADE_SHADE_ALTITUDE), shadeAltitudeCheckBox);
		BorderedTitledPane shade = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_SHADE, shadingGrid);

		GridPane smoothGrid = createGrid();
		addPairToGrid(smoothGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SMOOTH_SMOOTH_RENDERING), smoothRendering);
		addPairToGrid(smoothGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_SMOOTH_SMOOTH_OVERLAYS), smoothOverlays);
		BorderedTitledPane smooth = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_SMOOTH, smoothGrid);

		HBox.setHgrow(shade, Priority.ALWAYS);
		HBox.setHgrow(smooth, Priority.ALWAYS);
		shadingAndSmooth.getChildren().addAll(shade, smooth);

		GridPane layerGrid = createGrid();
		hSlider = new HeightSlider(tmpWorldCfg.getRenderHeight(), false);
		hSlider.valueProperty().set(tmpWorldCfg.getRenderHeight());
		hSlider.setMajorTickUnit(64);
		hSlider.setAlignment(Pos.CENTER_LEFT);
		addPairToGrid(layerGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_HEIGHT), hSlider);
		addPairToGrid(layerGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_LAYER_ONLY), layerOnly);
		addPairToGrid(layerGrid, 2, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_CAVES), caves);
		BorderedTitledPane layers = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_LAYERS, layerGrid);

		GridPane structureGrid = createGrid();
		structureIconSize = createIconSizeSlider(tmpWorldCfg.getStructureIconSize());
		structureIconSize.setValue(tmpWorldCfg.getStructureIconSize());
		structureIconBorderSize = createSlider(0, 5, 1, tmpWorldCfg.getStructureIconBorderSize());
		structureIconBorderSize.setValue(tmpWorldCfg.getStructureIconBorderSize());
		addPairToGrid(structureGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_STRUCTURE_ICON_SIZE), structureIconSize);
		addPairToGrid(structureGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_STRUCTURE_ICON_BORDER_SIZE), structureIconBorderSize);
		BorderedTitledPane structureIcons = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_STRUCTURE_ICONS, structureGrid);

		GridPane backgroundGrid = createGrid();
		addPairToGrid(backgroundGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_BACKGROUND_BACKGROUND_PATTERN), tileMapBackgrounds);
		addPairToGrid(backgroundGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_BACKGROUND_SHOW_NONEXISTENT_REGIONS), showNonexistentRegionsCheckBox);
		BorderedTitledPane background = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_BACKGROUND, backgroundGrid);

		GridPane zoomLevelGrid = createGrid();
		zoomLevelSlider = new ZoomLevelSlider(tmpWorldCfg.getRenderHeaderOnlyZoomLevel());
		addPairToGrid(zoomLevelGrid, 0, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_ZOOM_LEVEL_LOD), zoomLevelSlider);
		addPairToGrid(zoomLevelGrid, 1, UIFactory.label(Translation.DIALOG_SETTINGS_RENDERING_ZOOM_LEVEL_COLOR), zoomLevelLODColorPreview);
		BorderedTitledPane zoomLevel = new BorderedTitledPane(Translation.DIALOG_SETTINGS_RENDERING_ZOOM_LEVEL, zoomLevelGrid);

		zoomLevelLODColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), zoomLevelLODColor.get()).showColorPicker();
			result.ifPresent(c -> {
				zoomLevelLODColor.set(c);
				zoomLevelLODColorPreview.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
			});
		});

		renderingBox.getChildren().addAll(shadingAndSmooth, layers, structureIcons, background, zoomLevel);
		renderingScrollPane.setContent(renderingBox);
		renderingTab.setContent(renderingScrollPane);
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

		renderingTab.setDisable(tmpWorldCfg.getWorldDirs() == null);
		worldTab.setDisable(tmpWorldCfg.getWorldDirs() == null);
		renderingToggleButton.setDisable(tmpWorldCfg.getWorldDirs() == null);
		worldToggleButton.setDisable(tmpWorldCfg.getWorldDirs() == null);

		tabPane.getTabs().addAll(globalTab, processingTab, renderingTab, worldTab);

		final DataProperty<Tab> focusedTab = new DataProperty<>(globalTab);
		if (tmpWorldCfg.getWorldDirs() != null && renderSettings) {
			focusedTab.set(renderingTab);
			toggleGroup.selectToggle(renderingToggleButton);
		} else {
			toggleGroup.selectToggle(globalToggleButton);
		}

		Platform.runLater(() -> focusedTab.get().getContent().requestFocus());

		BorderPane tabBox = new BorderPane();
		tabBox.setLeft(leftTabs);
		tabBox.setRight(rightTabs);

		VBox content = new VBox();
		content.getChildren().addAll(tabBox, tabPane);
		getDialogPane().setContent(content);

		getDialogPane().lookupButton(reset).addEventFilter(ActionEvent.ACTION, e -> {
			e.consume();
			languages.setValue(GlobalConfig.DEFAULT_LOCALE);
			processThreadsSlider.setValue(GlobalConfig.DEFAULT_PROCESS_THREADS);
			writeThreadsSlider.setValue(GlobalConfig.DEFAULT_WRITE_THREADS);
			maxLoadedFilesSlider.setValue(GlobalConfig.DEFAULT_MAX_LOADED_FILES);
			regionSelectionColor.set(GlobalConfig.DEFAULT_REGION_SELECTION_COLOR.makeJavaFXColor());
			regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(GlobalConfig.DEFAULT_REGION_SELECTION_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			chunkSelectionColor.set(GlobalConfig.DEFAULT_CHUNK_SELECTION_COLOR.makeJavaFXColor());
			chunkSelectionColorPreview.setBackground(new Background(new BackgroundFill(GlobalConfig.DEFAULT_CHUNK_SELECTION_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			pasteChunksColor.set(GlobalConfig.DEFAULT_PASTE_CHUNKS_COLOR.makeJavaFXColor());
			pasteChunksColorPreview.setBackground(new Background(new BackgroundFill(GlobalConfig.DEFAULT_PASTE_CHUNKS_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			zoomLevelLODColor.set(WorldConfig.DEFAULT_ZOOM_LEVEL_LOD_COLOR.makeJavaFXColor());
			zoomLevelLODColorPreview.setBackground(new Background(new BackgroundFill(WorldConfig.DEFAULT_ZOOM_LEVEL_LOD_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			zoomLevelSlider.setZoomLevelValue(WorldConfig.DEFAULT_RENDER_HEADER_ONLY_ZOOM_LEVEL);
			shadeCheckBox.setSelected(WorldConfig.DEFAULT_SHADE);
			shadeWaterCheckBox.setSelected(WorldConfig.DEFAULT_SHADE_WATER);
			shadeAltitudeCheckBox.setSelected(WorldConfig.DEFAULT_SHADE_ALTITUDE);
			showNonexistentRegionsCheckBox.setSelected(WorldConfig.DEFAULT_SHOW_NONEXISTENT_REGIONS);
			smoothRendering.setSelected(WorldConfig.DEFAULT_SMOOTH_RENDERING);
			smoothOverlays.setSelected(WorldConfig.DEFAULT_SMOOTH_OVERLAYS);
			structureIconSize.setValue(WorldConfig.DEFAULT_STRUCTURE_ICON_SIZE);
			structureIconBorderSize.setValue(WorldConfig.DEFAULT_STRUCTURE_ICON_BORDER_SIZE);
			hSlider.valueProperty().set(hSlider.getValue());
			caves.setSelected(WorldConfig.DEFAULT_RENDER_CAVES);
			tileMapBackgrounds.setValue(TileMapBox.TileMapBoxBackground.valueOf(WorldConfig.DEFAULT_TILEMAP_BACKGROUND));
			mcSavesDir.setFile(GlobalConfig.DEFAULT_MC_SAVES_DIR == null ? null : new File(GlobalConfig.DEFAULT_MC_SAVES_DIR));
			debugCheckBox.setSelected(GlobalConfig.DEFAULT_DEBUG);
		});

		Runnable writeTmpCfg = () -> {
			tmpGlobalCfg.setLocale(languages.getSelectionModel().getSelectedItem());
			tmpGlobalCfg.setProcessThreads((int) processThreadsSlider.getValue());
			tmpGlobalCfg.setWriteThreads((int) writeThreadsSlider.getValue());
			tmpGlobalCfg.setMaxLoadedFiles((int) maxLoadedFilesSlider.getValue());
			tmpGlobalCfg.setRegionSelectionColor(new net.querz.mcaselector.ui.Color(regionSelectionColor.get()));
			tmpGlobalCfg.setChunkSelectionColor(new net.querz.mcaselector.ui.Color(chunkSelectionColor.get()));
			tmpGlobalCfg.setPasteChunksColor(new net.querz.mcaselector.ui.Color(pasteChunksColor.get()));
			tmpGlobalCfg.setMcSavesDir(Objects.requireNonNullElse(mcSavesDir.getFile().toString(), GlobalConfig.DEFAULT_MC_SAVES_DIR));
			tmpGlobalCfg.setDebug(debugCheckBox.isSelected());
			tmpWorldCfg.setShade(shadeCheckBox.isSelected());
			tmpWorldCfg.setShadeWater(shadeWaterCheckBox.isSelected());
			tmpWorldCfg.setShadeAltitude(shadeAltitudeCheckBox.isSelected());
			tmpWorldCfg.setShowNonexistentRegions(showNonexistentRegionsCheckBox.isSelected());
			tmpWorldCfg.setSmoothRendering(smoothRendering.isSelected());
			tmpWorldCfg.setSmoothOverlays(smoothOverlays.isSelected());
			tmpWorldCfg.setTileMapBackground(tileMapBackgrounds.getSelectionModel().getSelectedItem().name());
			tmpWorldCfg.setRenderHeaderOnlyZoomLevel(zoomLevelSlider.getZoomLevelValue());
			tmpWorldCfg.setZoomLevelLODColor(new net.querz.mcaselector.ui.Color(zoomLevelLODColor.get()));
			tmpWorldCfg.setRenderHeight(hSlider.getValue());
			tmpWorldCfg.setRenderLayerOnly(layerOnly.isSelected());
			tmpWorldCfg.setRenderCaves(caves.isSelected());
			tmpWorldCfg.setStructureIconSize((int) structureIconSize.getValue());
			tmpWorldCfg.setStructureIconBorderSize((int) structureIconBorderSize.getValue());
			if (tmpWorldCfg.getWorldDirs() != null) {
				tmpWorldCfg.getWorldDirs().setPoi(poiField.getFile());
				tmpWorldCfg.getWorldDirs().setEntities(entitiesField.getFile());
			}
		};

		Node setDefaultButton = getDialogPane().lookupButton(setDefault);
		setDefaultButton.addEventFilter(ActionEvent.ACTION, e -> {
			writeTmpCfg.run();
			ConfigProvider.DEFAULT_WORLD = tmpWorldCfg.copyForSettings();
			ConfigProvider.DEFAULT_WORLD.saveDefault();
		});
		setDefaultButton.setVisible(tabPane.getSelectionModel().getSelectedItem() == renderingTab);
		tabPane.getSelectionModel().selectedItemProperty().addListener((v, o, n) -> setDefaultButton.setVisible(n == renderingTab));

		setResultConverter(c -> {
			if (c.getButtonData() == ButtonBar.ButtonData.OK_DONE || c.getButtonData() == setDefault.getButtonData()) {
				writeTmpCfg.run();
				return new Result(tmpGlobalCfg, tmpWorldCfg);
			}
			return null;
		});

		getDialogPane().getStylesheets().add(Objects.requireNonNull(SettingsDialog.class.getClassLoader().getResource("style/component/settings-dialog.css")).toExternalForm());
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
		ColumnConstraints column1 = new ColumnConstraints();
		ColumnConstraints column2 = new ColumnConstraints();
		column2.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(column1, column2);
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
		int majorTicks = Math.max((max - min) / 5, 1);
		slider.setMajorTickUnit(majorTicks);
		slider.setMinorTickCount(majorTicks - 1);
		slider.setBlockIncrement(steps);
		return slider;
	}

	private Slider createIconSizeSlider(int init) {
		Slider slider = new Slider(16, 64, init);
		slider.setMajorTickUnit(16);
		slider.setMinorTickCount(0);
		slider.setBlockIncrement(16);
		return slider;
	}

	public record Result(GlobalConfig tmpGlobalConfig, WorldConfig tmpWorldConfig) {}

	// overwrite DialogPane to set a custom button order
	private static class CustomOrderDialogPane extends DialogPane {
		@Override
		protected ButtonBar createButtonBar() {
			ButtonBar buttonBar = (ButtonBar) super.createButtonBar();
			// L -> Reset
			// A -> Set default
			// O -> OK
			// C -> Cancel
			buttonBar.setButtonOrder("L+A_OC");
			return buttonBar;
		}
	}
}
