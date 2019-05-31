package net.querz.mcaselector.ui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class ImportConfirmationDialog extends ConfirmationDialog {

	public ImportConfirmationDialog(Stage primaryStage, Consumer<Boolean> overwriteAction) {
		super(
				primaryStage,
				"Import chunks",
				"You are about to import an unknown number of chunks to this world.",
				"import"
		);

		CheckBox overwrite = new CheckBox("Overwrite existing chunks");
		overwrite.setSelected(true);
		overwrite.setOnAction(e -> overwriteAction.accept(overwrite.isSelected()));

		BorderedTitledPane options = new BorderedTitledPane("Options", overwrite);

		Label contentLabel = new Label(getContentText());
		VBox content = new VBox();
		content.getStyleClass().add("import-confirmation-dialog-content");
		content.getChildren().addAll(options, contentLabel);
		getDialogPane().setContent(content);
	}
}
