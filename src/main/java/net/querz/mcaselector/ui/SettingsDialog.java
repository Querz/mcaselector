package net.querz.mcaselector.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Translation;
import net.querz.mcaselector.util.UIFactory;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SettingsDialog extends Dialog<SettingsDialog.Result> {

	/*
	* Region selection color and opacity
	* Chunk selection color and opacity
	* MCAFilePipe thread options:
	* - Number of threads for file reading
	* - Number of threads for processing
	* - Number of threads for writing
	* - Maximum amount of loaded files
	* toggle debug
	* */

	private static final int procCount = Runtime.getRuntime().availableProcessors();
	private static final long maxMem = Runtime.getRuntime().maxMemory();

	private ComboBox<Locale> languages = new ComboBox<>();

	private Slider readThreadsSlider = createSlider(1, procCount, 1, 1);
	private Slider processThreadsSlider = createSlider(1, procCount * 2, 1, procCount);
	private Slider writeThreadsSlider = createSlider(1, procCount, 1, procCount < 4 ? procCount : 4);
	private Slider maxLoadedFilesSlider = createSlider(1, (int) Math.ceil(maxMem / 100_000_000L), 1, procCount + procCount / 2);
	private Button regionSelectionColorPreview = new Button();
	private Button chunkSelectionColorPreview = new Button();
	private CheckBox debugCheckBox = new CheckBox();

	private Color regionSelectionColor = Config.getRegionSelectionColor().makeJavaFXColor();
	private Color chunkSelectionColor = Config.getChunkSelectionColor().makeJavaFXColor();

	private ButtonType reset = new ButtonType(Translation.DIALOG_SETTINGS_RESET.toString(), ButtonBar.ButtonData.LEFT);

	public SettingsDialog(Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_SETTINGS_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("settings-dialog-pane");
		getDialogPane().getScene().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL, reset);

		getDialogPane().lookupButton(reset).addEventFilter(ActionEvent.ACTION, e -> {
			e.consume();
			languages.setValue(Config.DEFAULT_LOCALE);
			readThreadsSlider.setValue(Config.DEFAULT_LOAD_THREADS);
			processThreadsSlider.setValue(Config.DEFAULT_PROCESS_THREADS);
			writeThreadsSlider.setValue(Config.DEFAULT_WRITE_THREADS);
			maxLoadedFilesSlider.setValue(Config.DEFAULT_MAX_LOADED_FILES);
			regionSelectionColor = Config.DEFAULT_REGION_SELECTION_COLOR.makeJavaFXColor();
			regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(Config.DEFAULT_REGION_SELECTION_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			chunkSelectionColor = Config.DEFAULT_CHUNK_SELECTION_COLOR.makeJavaFXColor();
			chunkSelectionColorPreview.setBackground(new Background(new BackgroundFill(Config.DEFAULT_CHUNK_SELECTION_COLOR.makeJavaFXColor(), CornerRadii.EMPTY, Insets.EMPTY)));
			debugCheckBox.setSelected(Config.DEFAULT_DEBUG);
		});

		setResultConverter(c -> {
			if (c.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
				return new SettingsDialog.Result(
					languages.getSelectionModel().getSelectedItem(),
					(int) readThreadsSlider.getValue(),
					(int) processThreadsSlider.getValue(),
					(int) writeThreadsSlider.getValue(),
					(int) maxLoadedFilesSlider.getValue(),
					regionSelectionColor,
					chunkSelectionColor,
					debugCheckBox.isSelected()
				);
			}
			return null;
		});

		languages.getItems().addAll(Translation.getAvailableLanguages());
		languages.setValue(Config.getLocale());
		languages.setConverter(new StringConverter<Locale>() {

			Map<String, Locale> cache = new HashMap<>();

			@Override
			public String toString(Locale locale) {
				String display = locale.getDisplayName();
				cache.put(display, locale);
				return display;
			}

			@Override
			public Locale fromString(String string) {
				return cache.get(string);
			}
		});
		languages.getStyleClass().add("languages-combo-box");

		readThreadsSlider.setValue(Config.getLoadThreads());
		processThreadsSlider.setValue(Config.getProcessThreads());
		writeThreadsSlider.setValue(Config.getWriteThreads());
		maxLoadedFilesSlider.setValue(Config.getMaxLoadedFiles());

		regionSelectionColorPreview.getStyleClass().clear();
		chunkSelectionColorPreview.getStyleClass().clear();
		regionSelectionColorPreview.getStyleClass().add("color-preview-button");
		chunkSelectionColorPreview.getStyleClass().add("color-preview-button");
		regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(regionSelectionColor, CornerRadii.EMPTY, Insets.EMPTY)));
		chunkSelectionColorPreview.setBackground(new Background(new BackgroundFill(chunkSelectionColor, CornerRadii.EMPTY, Insets.EMPTY)));
		debugCheckBox.setSelected(Config.debug());

		regionSelectionColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), regionSelectionColor).showColorPicker();
			result.ifPresent(c -> {
				regionSelectionColor = c;
				regionSelectionColorPreview.setBackground(new Background(new BackgroundFill(c, CornerRadii.EMPTY, Insets.EMPTY)));
			});
		});
		chunkSelectionColorPreview.setOnMousePressed(e -> {
			Optional<Color> result = new ColorPicker(getDialogPane().getScene().getWindow(), chunkSelectionColor).showColorPicker();
			result.ifPresent(c -> chunkSelectionColor = c);
		});

		GridPane grid = new GridPane();
		grid.getStyleClass().add("slider-grid-pane");
		grid.add(UIFactory.label(Translation.DIALOG_SETTINGS_LANGUAGE), 0, 0, 1, 1);
		grid.add(UIFactory.label(Translation.DIALOG_SETTINGS_READ_THREADS), 0, 1, 1, 1);
		grid.add(UIFactory.label(Translation.DIALOG_SETTINGS_PROCESS_THREADS), 0, 2, 1, 1);
		grid.add(UIFactory.label(Translation.DIALOG_SETTINGS_WRITE_THREADS), 0, 3, 1, 1);
		grid.add(UIFactory.label(Translation.DIALOG_SETTINGS_MAX_FILES), 0, 4, 1, 1);
		grid.add(UIFactory.label(Translation.DIALOG_SETTINGS_REGION_COLOR), 0, 5, 1, 1);
		grid.add(UIFactory.label(Translation.DIALOG_SETTINGS_CHUNK_COLOR), 0, 6, 1, 1);
		grid.add(UIFactory.label(Translation.DIALOG_SETTINGS_PRINT_DEBUG), 0, 7, 1, 1);
		grid.add(languages, 1, 0, 2, 1);
		grid.add(readThreadsSlider, 1, 1, 1, 1);
		grid.add(processThreadsSlider, 1, 2, 1, 1);
		grid.add(writeThreadsSlider, 1, 3, 1, 1);
		grid.add(maxLoadedFilesSlider, 1, 4, 1, 1);
		grid.add(regionSelectionColorPreview, 1, 5, 2, 1);
		grid.add(chunkSelectionColorPreview, 1, 6, 2, 1);
		grid.add(debugCheckBox, 1, 7, 2, 1);
		grid.add(Helper.attachTextFieldToSlider(readThreadsSlider), 2, 1, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(processThreadsSlider), 2, 2, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(writeThreadsSlider), 2, 3, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(maxLoadedFilesSlider), 2, 4, 1, 1);

		getDialogPane().setContent(grid);
	}

	private Slider createSlider(int min, int max, int steps, int init) {
		Slider slider = new Slider(min, max, init);
		slider.setMajorTickUnit(steps);
		slider.setMinorTickCount(0);
		slider.setBlockIncrement(steps);
		return slider;
	}

	public class Result {

		private int readThreads, processThreads, writeThreads, maxLoadedFiles;
		private Color regionColor, chunkColor;
		private boolean debug;
		private Locale locale;

		public Result(Locale locale, int readThreads, int processThreads, int writeThreads, int maxLoadedFiles, Color regionColor, Color chunkColor, boolean debug) {
			this.locale = locale;
			this.readThreads = readThreads;
			this.processThreads = processThreads;
			this.writeThreads = writeThreads;
			this.maxLoadedFiles = maxLoadedFiles;
			this.regionColor = regionColor;
			this.chunkColor = chunkColor;
			this.debug = debug;
		}

		public Locale getLocale() {
			return locale;
		}

		public int getReadThreads() {
			return readThreads;
		}

		public int getProcessThreads() {
			return processThreads;
		}

		public int getWriteThreads() {
			return writeThreads;
		}

		public int getMaxLoadedFiles() {
			return maxLoadedFiles;
		}

		public Color getRegionColor() {
			return regionColor;
		}

		public Color getChunkColor() {
			return chunkColor;
		}

		public boolean getDebug() {
			return debug;
		}
	}
}
