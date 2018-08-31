package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

public abstract class ProgressTask extends Task<Void> {

	public int max;
	private int current = 0;
	private StringProperty infoProperty = new SimpleStringProperty("running...");
	private Runnable onFinish;

	public ProgressTask() {}

	public ProgressTask(int max) {
		this.max = max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public void incrementProgress(String info) {
		updateProgress(info, ++current, max);
	}

	public void updateProgress(String info, double progress) {
		updateProgress(info, progress, max);
	}

	public void updateProgress(String info, double progress, int max) {
		Platform.runLater(() -> infoProperty.setValue(info));
		updateProgress(progress, max);
		if (progress == max && onFinish != null) {
			Platform.runLater(onFinish);
		}
	}

	public StringProperty infoProperty() {
		return infoProperty;
	}

	public void setOnFinish(Runnable r) {
		onFinish = r;
	}

	public static class Dummy extends ProgressTask {

		@Override
		public void updateProgress(String info, double progress, int max) {

		}

		@Override
		protected Void call() {
			return null;
		}
	}
}
