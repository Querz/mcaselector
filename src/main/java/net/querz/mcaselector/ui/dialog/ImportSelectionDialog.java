package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;

public class ImportSelectionDialog extends Dialog<ImportSelectionDialog.Result> {


	private final RadioButton overwrite = UIFactory.radio(Translation.DIALOG_IMPORT_SELECTION_OVERWRITE, Result.OVERWRITE);
	private final RadioButton merge = UIFactory.radio(Translation.DIALOG_IMPORT_SELECTION_MERGE, Result.MERGE);
	private final RadioButton subtract = UIFactory.radio(Translation.DIALOG_IMPORT_SELECTION_SUBTRACT, Result.SUBTRACT);

	public ImportSelectionDialog(Stage primaryStage) {
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("import-selection-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		titleProperty().bind(Translation.DIALOG_IMPORT_SELECTION_TITLE.getProperty());
		headerTextProperty().bind(Translation.DIALOG_IMPORT_SELECTION_HEADER.getProperty());
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		ToggleGroup toggleGroup = new ToggleGroup();
		toggleGroup.getToggles().addAll(overwrite, merge, subtract);
		merge.fire();

		setResultConverter(p -> p == ButtonType.OK ? (Result) toggleGroup.getSelectedToggle().getUserData() : null);

		HBox choices = new HBox();
		choices.getChildren().addAll(overwrite, merge, subtract);

		getDialogPane().setContent(choices);
	}

	public enum Result {
		OVERWRITE, MERGE, SUBTRACT
	}
}
