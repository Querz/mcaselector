package net.querz.mcaselector.ui;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.io.MCAFilePipe;

import java.util.function.Consumer;

public class ProgressDialog extends Stage {

	private Label title = new Label("Progress");
	private ProgressBar progressBar = new ProgressBar(-1);
	private Label label = new Label("running...");
	private Button cancel = new Button("Cancel");

	private ProgressTask currentTask;

	public ProgressDialog(String title, Stage primaryStage) {
		initStyle(StageStyle.TRANSPARENT);
		setResizable(false);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(primaryStage);

		this.title.setText(title);
		this.title.getStyleClass().add("progress-title");

		label.getStyleClass().add("progress-info");

		HBox cancelBox = new HBox();
		cancelBox.getStyleClass().add("cancel-box");
		cancelBox.getChildren().add(cancel);

		VBox box = new VBox();
		box.getStyleClass().add("progress-dialog");
		box.getChildren().addAll(this.title, progressBar, label, cancelBox);
		cancel.setOnAction(e -> {
			currentTask.setLocked(true);
			currentTask.setIndeterminate("cancelling...");
			MCAFilePipe.cancelAllJobs(() -> {
				currentTask.done("done.");
				close();
			});
		});

		progressBar.prefWidthProperty().bind(box.widthProperty());
		this.title.prefWidthProperty().bind(box.widthProperty());
		label.prefWidthProperty().bind(box.widthProperty());

		Scene scene = new Scene(box);
		scene.setFill(Color.TRANSPARENT);

		scene.getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		setScene(scene);
	}

	public void showProgressBar(Consumer<ProgressTask> r) {
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
	}

	public void updateProgress(String status, double progress) {
		progressBar.setProgress(progress);
		label.setText(status);
	}
}
