package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;

public class ImportSelectionDialog extends Dialog<ImportSelectionDialog.Result> {


	private final RadioButton overwrite = UIFactory.radio(Translation.DIALOG_IMPORT_SELECTION_OVERWRITE);
	private final RadioButton merge = UIFactory.radio(Translation.DIALOG_IMPORT_SELECTION_MERGE);

	public ImportSelectionDialog(Stage primaryStage) {
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("import-selection-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		titleProperty().bind(Translation.DIALOG_IMPORT_SELECTION_TITLE.getProperty());
		headerTextProperty().bind(Translation.DIALOG_IMPORT_SELECTION_HEADER.getProperty());

		setResultConverter(p -> p == ButtonType.OK ? overwrite.isSelected() ? Result.OVERWRITE : Result.MERGE : null);
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		ToggleGroup toggleGroup = new ToggleGroup();
		toggleGroup.getToggles().addAll(overwrite, merge);
		merge.fire();

		HBox choices = new HBox();
		choices.getChildren().addAll(overwrite, merge);

		getDialogPane().setContent(choices);
	}

	public enum Result {
		OVERWRITE, MERGE
	}
}
