package net.querz.mcaselector.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import net.querz.mcaselector.text.Translation;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ImportConfirmationDialog extends ConfirmationDialog {

	public ImportConfirmationDialog(Stage primaryStage, ChunkImportConfirmationData preFill, Consumer<ChunkImportConfirmationData> dataAction) {
		super(
				primaryStage,
				Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_TITLE,
				Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_HEADER,
				"import"
		);

		ChunkImportConfirmationData data = new ChunkImportConfirmationData();

		Label warning = UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_WARNING);
		warning.getStyleClass().add("import-chunks-warning");
		warning.getStyleClass().add("import-chunks-warning-invisible");
		DataProperty<Boolean> warningVisible = new DataProperty<>();
		warningVisible.set(false);

		LocationInput locationInput = new LocationInput(true);
		locationInput.setOnValidityCheck(valid -> {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
			data.offset = locationInput.getValue();
			dataAction.accept(data);
			if (valid && (data.offset.getX() != 0 || data.offset.getY() != 0)) {
				warning.getStyleClass().remove("import-chunks-warning-invisible");
				warningVisible.set(true);
			} else if (warningVisible.get()) {
				warning.getStyleClass().add("import-chunks-warning-invisible");
				warningVisible.set(false);
			}
		});

		CheckBox overwrite = new CheckBox();
		overwrite.setOnAction(e -> {
			data.overwrite = overwrite.isSelected();
			dataAction.accept(data);
		});

		CheckBox selectionOnly = new CheckBox();
		selectionOnly.setOnAction(e -> {
			data.selectionOnly = selectionOnly.isSelected();
			dataAction.accept(data);
		});

		TextField range = new TextField();
		range.textProperty().addListener((obs, o, n) -> {
			if (n.isEmpty()) {
				data.ranges = null;
			} else {
				List<Range> ranges = RangeParser.parseRanges(n, ",");
				if (ranges == null) {
					range.setText(o);
				} else {
					data.ranges = ranges;
				}
			}
		});

		overwrite.setSelected(true);

		data.offset = locationInput.getValue();
		data.overwrite = true;
		dataAction.accept(data);

		GridPane optionGrid = new GridPane();
		optionGrid.getStyleClass().add("import-chunks-options-grid");
		optionGrid.add(UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OFFSET), 0, 0);
		optionGrid.add(locationInput, 1, 0);
		optionGrid.add(UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OVERWRITE), 0, 1);
		optionGrid.add(overwrite, 1, 1);
		optionGrid.add(UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_SELECTION_ONLY), 0, 2);
		optionGrid.add(selectionOnly, 1, 2);
		optionGrid.add(UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_SECTIONS), 0, 3);
		optionGrid.add(range, 1, 3);

		BorderedTitledPane options = new BorderedTitledPane(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS, optionGrid);

		Label contentLabel = UIFactory.label(Translation.DIALOG_CONFIRMATION_QUESTION);
		contentLabel.getStyleClass().add("import-chunks-confirmation-label");
		VBox content = new VBox();
		content.getStyleClass().add("import-confirmation-dialog-content");
		VBox confirmationLabels = new VBox();
		confirmationLabels.getStyleClass().add("v-box");
		confirmationLabels.getChildren().addAll(contentLabel, warning);
		content.getChildren().addAll(options, confirmationLabels);
		getDialogPane().setContent(content);

		if (preFill != null) {
			if (preFill.offset != null) {
				data.offset = preFill.offset;
				locationInput.setX(preFill.offset.getX());
				locationInput.setZ(preFill.offset.getY());
			}
			data.overwrite = preFill.overwrite;
			overwrite.setSelected(preFill.overwrite);
			data.selectionOnly = preFill.selectionOnly;
			selectionOnly.setSelected(preFill.selectionOnly);
			if (preFill.ranges != null) {
				data.ranges = preFill.ranges;
				range.setText(preFill.ranges.stream().map(Range::toString).collect(Collectors.joining(",")));
			}
		}
	}

	public static class ChunkImportConfirmationData {

		private Point2i offset;
		private boolean overwrite;
		private boolean selectionOnly;
		private List<Range> ranges;

		private ChunkImportConfirmationData() {}

		public ChunkImportConfirmationData(Point2i offset, boolean overwrite, boolean selectionOnly, List<Range> ranges) {
			this.offset = offset;
			this.overwrite = overwrite;
			this.selectionOnly = selectionOnly;
			this.ranges = ranges;
		}

		public Point2i getOffset() {
			return offset;
		}

		public boolean overwrite() {
			return overwrite;
		}

		public boolean selectionOnly() {
			return selectionOnly;
		}

		public List<Range> getRanges() {
			return ranges;
		}
	}
}
