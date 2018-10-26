package net.querz.mcaselector.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.util.Helper;

//does not return something, but sets the configuration directly
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

	private Slider readThreadsSlider = createSlider(1, procCount, 1, 1);
	private Slider processThreadsSlider = createSlider(1, procCount * 2, 1, procCount);
	private Slider writeThreadsSlider = createSlider(1, procCount, 1, procCount < 4 ? procCount : 4);
	private Slider maxLoadedFilesSlider = createSlider(1, (int) Math.ceil(maxMem / 100_000_000L), 1, procCount + procCount / 2);
	private StackPane regionSelectionColor = new StackPane();
	private StackPane chunkSelectionColor = new StackPane();

	public SettingsDialog(Stage primaryStage) {
		setTitle("Edit settings");
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("settings-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResultConverter(c -> new Result(
				(int) readThreadsSlider.getValue(),
				(int) processThreadsSlider.getValue(),
				(int) writeThreadsSlider.getValue(),
				(int) maxLoadedFilesSlider.getValue(),
				new Color(1, 0, 0, 0.8),
				new Color(1, 0.45, 0, 0.8)
		));

		GridPane grid = new GridPane();
		grid.getStyleClass().add("slider-grid-pane");
		grid.add(new Label("Read Threads"), 0, 0, 1, 1);
		grid.add(new Label("Process Threads"), 0, 1, 1, 1);
		grid.add(new Label("Write Threads"), 0, 2, 1, 1);
		grid.add(new Label("Max loaded Files"), 0, 3, 1, 1);
		grid.add(readThreadsSlider, 1, 0, 1, 1);
		grid.add(processThreadsSlider, 1, 1, 1, 1);
		grid.add(writeThreadsSlider, 1, 2, 1, 1);
		grid.add(maxLoadedFilesSlider, 1, 3, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(readThreadsSlider), 2, 0, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(processThreadsSlider), 2, 1, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(writeThreadsSlider), 2, 2, 1, 1);
		grid.add(Helper.attachTextFieldToSlider(maxLoadedFilesSlider), 2, 3, 1, 1);

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

		public Result(int readThreads, int processThreads, int writeThreads, int maxLoadedFiles, Color regionColor, Color chunkColor) {
			this.readThreads = readThreads;
			this.processThreads = processThreads;
			this.writeThreads = writeThreads;
			this.maxLoadedFiles = maxLoadedFiles;
			this.regionColor = regionColor;
			this.chunkColor = chunkColor;
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
	}
}
