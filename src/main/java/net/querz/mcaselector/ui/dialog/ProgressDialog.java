package net.querz.mcaselector.ui.dialog;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.ui.UIFactory;
import java.util.function.Consumer;

public class ProgressDialog extends Stage {

	private final Label title = new Label();
	private final ProgressBar progressBar = new ProgressBar(-1);
	private final Label label = UIFactory.label(Translation.DIALOG_PROGRESS_RUNNING);

	private ProgressTask currentTask;

	private final VBox box = new VBox();

	public ProgressDialog(Translation title, Window primaryStage) {
		initStyle(StageStyle.TRANSPARENT);
		setResizable(false);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(primaryStage);

		this.title.textProperty().bind(title.getProperty());
		this.title.getStyleClass().add("progress-title");

		label.getStyleClass().add("progress-info");

		box.getStyleClass().add("progress-dialog");
		box.getChildren().addAll(this.title, progressBar, label);

		progressBar.prefWidthProperty().bind(box.widthProperty());
		this.title.prefWidthProperty().bind(box.widthProperty());
		label.prefWidthProperty().bind(box.widthProperty());

		Scene scene = new Scene(box);
		scene.setFill(Color.TRANSPARENT);

		scene.getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		scene.getStylesheets().add(ProgressDialog.class.getClassLoader().getResource("style/component/progress-dialog.css").toExternalForm());

		setScene(scene);
	}

	public void showProgressBar(Consumer<ProgressTask> r) {
		JobHandler.setTrimSaveData(false);
		currentTask = new ProgressTask() {
			@Override
			protected Void call() {
				r.accept(this);
				return null;
			}
		};
		progressBar.progressProperty().bind(currentTask.progressProperty());
		label.textProperty().bind(currentTask.infoProperty());
		currentTask.setOnFinish(this::close);
		Thread thread = new Thread(currentTask);
		thread.start();
		showAndWait();
		JobHandler.setTrimSaveData(true);
	}

	public void updateProgress(String status, double progress) {
		progressBar.setProgress(progress);
		label.setText(status);
	}

	protected VBox getBox() {
		return box;
	}

	protected ProgressTask getCurrentTask() {
		return currentTask;
	}
}
