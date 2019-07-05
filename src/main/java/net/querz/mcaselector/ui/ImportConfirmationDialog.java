package net.querz.mcaselector.ui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.util.Translation;
import net.querz.mcaselector.util.UIFactory;
import java.util.function.Consumer;

public class ImportConfirmationDialog extends ConfirmationDialog {

	public ImportConfirmationDialog(Stage primaryStage, Consumer<Boolean> overwriteAction) {
		super(
				primaryStage,
				Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_TITLE,
				Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_HEADER,
				"import"
		);

		CheckBox overwrite = UIFactory.checkbox(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OVERWRITE);
		overwrite.setOnAction(e -> overwriteAction.accept(overwrite.isSelected()));
		setOverwriteCheckbox(overwrite, overwriteAction, true);

		BorderedTitledPane options = new BorderedTitledPane(Translation.DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS, overwrite);

		Label contentLabel = UIFactory.label(Translation.DIALOG_CONFIRMATION_QUESTION);
		VBox content = new VBox();
		content.getStyleClass().add("import-confirmation-dialog-content");
		content.getChildren().addAll(options, contentLabel);
		getDialogPane().setContent(content);
	}

	private void setOverwriteCheckbox(CheckBox checkbox, Consumer<Boolean> action, boolean status) {
		// we need to run this after setting overwrite to selected to export the value
		// for the case when the user didn't interact with the checkbox.
		checkbox.setSelected(status);
		action.accept(status);
	}
}
