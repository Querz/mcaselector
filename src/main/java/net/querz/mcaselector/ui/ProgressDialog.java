package net.querz.mcaselector.ui;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

public class ProgressDialog extends Stage {

	private Label title = new Label("Progress");
	private ProgressBar progressBar = new ProgressBar(-1);
	private Label label = new Label("running...");

	public ProgressDialog(String title, Stage primaryStage) {
		initStyle(StageStyle.TRANSPARENT);
		setResizable(false);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(primaryStage);

		this.title.setText(title);
		this.title.getStyleClass().add("progress-title");

		label.getStyleClass().add("progress-info");

		VBox box = new VBox();
		box.getStyleClass().add("progress-dialog");
		box.getChildren().addAll(this.title, progressBar, label);

		progressBar.prefWidthProperty().bind(box.widthProperty());
		this.title.prefWidthProperty().bind(box.widthProperty());
		label.prefWidthProperty().bind(box.widthProperty());

		Scene scene = new Scene(box);
		scene.setFill(Color.TRANSPARENT);

		scene.getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		setScene(scene);
	}

	public void showProgressBar(Consumer<ProgressTask> r) {
		ProgressTask task = new ProgressTask() {
			@Override
			protected Void call() {
				r.accept(this);
				return null;
			}
		};
		progressBar.progressProperty().bind(task.progressProperty());
		label.textProperty().bind(task.infoProperty());
		task.setOnSucceeded(e -> close());
		Thread thread = new Thread(task);
		thread.start();
		showAndWait();
	}

	public void updateProgress(String status, double progress) {
		progressBar.setProgress(progress);
		label.setText(status);
	}
}
