package net.querz.mcaselector;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

public abstract class ProgressTask extends Task<Void> {

	private StringProperty infoProperty = new SimpleStringProperty("running...");

	public void updateProgress(String info, double progress, double max) {
		Platform.runLater(() -> infoProperty.setValue(info));
		updateProgress(progress, max);
	}

	public StringProperty infoProperty() {
		return infoProperty;
	}

	public static class Dummy extends ProgressTask {

		@Override
		public void updateProgress(String info, double progress, double max) {

		}

		@Override
		protected Void call() throws Exception {
			return null;
		}
	}
}
