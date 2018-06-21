package net.querz.mcaselector;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class ProgressDialog extends Alert {

	private ProgressBar progressBar = new ProgressBar(-1);
	private Label label = new Label("running...");

	public ProgressDialog() {
		super(AlertType.INFORMATION);
		getDialogPane().getChildren().addAll(progressBar, label);
	}

	public void updateProgress(String status, double progress) {
		progressBar.setProgress(progress);
		label.setText(status);
	}
}
