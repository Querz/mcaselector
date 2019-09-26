package net.querz.mcaselector.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import java.util.function.Consumer;

public class ImportConfirmationDialog extends ConfirmationDialog {

	public ImportConfirmationDialog(Stage primaryStage, Consumer<ChunkImportConfirmationData> dataAction) {
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

	}

	private void setOverwriteCheckbox(CheckBox checkbox, Consumer<ChunkImportConfirmationData> action, ChunkImportConfirmationData data, boolean status) {
		// we need to run this after setting overwrite to selected to export the value
		// for the case when the user didn't interact with the checkbox.
		checkbox.setSelected(status);
		data.overwrite = status;
		action.accept(data);
	}

	public class ChunkImportConfirmationData {

		private Point2i offset;
		private boolean overwrite;
		private boolean selectionOnly;

		public Point2i getOffset() {
			return offset;
		}

		public boolean overwrite() {
			return overwrite;
		}

		public boolean selectionOnly() {
			return selectionOnly;
		}
	}
}
