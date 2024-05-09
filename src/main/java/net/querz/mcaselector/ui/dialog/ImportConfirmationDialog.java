package net.querz.mcaselector.ui.dialog;

import javafx.css.PseudoClass;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.component.BorderedTitledPane;
import net.querz.mcaselector.ui.component.LocationInput;
import net.querz.mcaselector.ui.UIFactory;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ImportConfirmationDialog extends ConfirmationDialog {

	private static final PseudoClass invalid = PseudoClass.getPseudoClass("invalid");

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
			data.xzOffset = locationInput.getValue();
			dataAction.accept(data);
			if (valid && (data.xzOffset.getX() != 0 || data.xzOffset.getZ() != 0)) {
				warning.getStyleClass().remove("import-chunks-warning-invisible");
				warningVisible.set(true);
			} else if (warningVisible.get()) {
				warning.getStyleClass().add("import-chunks-warning-invisible");
				warningVisible.set(false);
			}
		});

		TextField yOffsetInput = new TextField();
		yOffsetInput.textProperty().addListener((v, o, n) -> {
			if (o != null && !o.equals(n)) {
				try {
					data.yOffset = Integer.parseInt(n);
					yOffsetInput.pseudoClassStateChanged(invalid, false);
				} catch (NumberFormatException ex) {
					data.yOffset = 0;
					yOffsetInput.pseudoClassStateChanged(invalid, n != null && !n.isEmpty());
				}
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
				range.pseudoClassStateChanged(invalid, false);
				data.ranges = null;
			} else {
				List<Range> ranges = RangeParser.parseRanges(n, ",");
				if (ranges == null) {
					range.pseudoClassStateChanged(invalid, true);
					data.ranges = null;
				} else {
					range.pseudoClassStateChanged(invalid, false);
					data.ranges = ranges;
				}
			}
		});

		overwrite.setSelected(true);

		data.xzOffset = locationInput.getValue();
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
		optionGrid.add(UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_Y_OFFSET), 0, 3);
		optionGrid.add(yOffsetInput, 1, 3);
		optionGrid.add(UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_SECTIONS), 0, 4);
		optionGrid.add(range, 1, 4);

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
			if (preFill.xzOffset != null) {
				data.xzOffset = preFill.xzOffset;
				locationInput.setX(preFill.xzOffset.getX());
				locationInput.setZ(preFill.xzOffset.getZ());
			}
			data.overwrite = preFill.overwrite;
			overwrite.setSelected(preFill.overwrite);
			data.selectionOnly = preFill.selectionOnly;
			selectionOnly.setSelected(preFill.selectionOnly);
			data.yOffset = preFill.yOffset;
			yOffsetInput.setText("" + preFill.yOffset);
			if (preFill.ranges != null) {
				data.ranges = preFill.ranges;
				range.setText(preFill.ranges.stream().map(Range::toString).collect(Collectors.joining(",")));
			}
		}

		getDialogPane().getStylesheets().add(ImportConfirmationDialog.class.getClassLoader().getResource("style/component/import-chunks-confirmation-dialog.css").toExternalForm());
	}

	public static class ChunkImportConfirmationData {

		private Point2i xzOffset;
		private int yOffset;
		private boolean overwrite;
		private boolean selectionOnly;
		private List<Range> ranges;

		private ChunkImportConfirmationData() {}

		public ChunkImportConfirmationData(Point2i xzOffset, int yOffset, boolean overwrite, boolean selectionOnly, List<Range> ranges) {
			this.xzOffset = xzOffset;
			this.yOffset = yOffset;
			this.overwrite = overwrite;
			this.selectionOnly = selectionOnly;
			this.ranges = ranges;
		}

		public Point3i getOffset() {
			return new Point3i(xzOffset.getX(), yOffset, xzOffset.getZ());
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
