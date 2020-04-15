package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.text.Translation;

public class CancellableProgressDialog extends ProgressDialog {

	private Button cancel = UIFactory.button(Translation.BUTTON_CANCEL);

	public CancellableProgressDialog(Translation title, Stage primaryStage) {
		super(title, primaryStage);

		HBox cancelBox = new HBox();
		cancelBox.getStyleClass().add("cancel-box");
		cancelBox.getChildren().add(cancel);

		getBox().getChildren().add(cancelBox);
		cancel.setOnAction(e -> {
			getCurrentTask().setLocked(true);
			getCurrentTask().setIndeterminate(Translation.DIALOG_PROGRESS_CANCELLING.toString());
			MCAFilePipe.cancelAllJobs(() -> Platform.runLater(() -> {
				getCurrentTask().done(Translation.DIALOG_PROGRESS_DONE.toString());
				close();
			}));
		});
	}
}
