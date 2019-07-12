package net.querz.mcaselector.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Translation;
import net.querz.mcaselector.util.UIFactory;
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

		LocationInput locationInput = new LocationInput(true);
		locationInput.setOnValidityCheck(valid -> {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
			data.offset = locationInput.getValue();
			dataAction.accept(data);
		});

		CheckBox overwrite = new CheckBox();
		overwrite.setOnAction(e -> {
			data.overwrite = overwrite.isSelected();
			dataAction.accept(data);
		});

		overwrite.setSelected(true);
		data.overwrite = true;
		dataAction.accept(data);

		GridPane optionGrid = new GridPane();
		optionGrid.getStyleClass().add("import-chunks-options-grid");
		optionGrid.add(UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OFFSET), 0, 0);
		optionGrid.add(locationInput, 1, 0);
		optionGrid.add(UIFactory.label(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OVERWRITE), 0, 1);
		optionGrid.add(overwrite, 1, 1);

		BorderedTitledPane options = new BorderedTitledPane(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS, optionGrid);

		Label contentLabel = UIFactory.label(Translation.DIALOG_CONFIRMATION_QUESTION);
		VBox content = new VBox();
		content.getStyleClass().add("import-confirmation-dialog-content");
		content.getChildren().addAll(options, contentLabel);
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

		public Point2i getOffset() {
			return offset;
		}

		public boolean overwrite() {
			return overwrite;
		}
	}
}
