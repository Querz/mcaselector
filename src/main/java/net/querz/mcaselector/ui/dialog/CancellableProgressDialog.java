package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;

public class CancellableProgressDialog extends ProgressDialog {

	enum CancellationScope {
		GLOBAL_JOBS,
		CURRENT_TASK
	}

	private boolean cancelled = false;
	private final CancellationScope cancellationScope;

	public CancellableProgressDialog(Translation title, Stage primaryStage) {
		this(title, primaryStage, CancellationScope.GLOBAL_JOBS);
	}

	CancellableProgressDialog(Translation title, Stage primaryStage, CancellationScope cancellationScope) {
		super(title, primaryStage);
		this.cancellationScope = cancellationScope;

		HBox cancelBox = new HBox();
		cancelBox.getStyleClass().add("cancel-box");
		Button cancel = UIFactory.button(Translation.BUTTON_CANCEL);
		cancelBox.getChildren().add(cancel);

		getBox().getChildren().add(cancelBox);
		cancel.setOnAction(e -> {
			cancel.setDisable(true);
			cancelled = true;
			getCurrentTask().setLocked(true);
			getCurrentTask().setIndeterminate(Translation.DIALOG_PROGRESS_CANCELLING.toString());
			getCurrentTask().cancelTask();
			cancellationAction(
					cancellationScope,
					() -> JobHandler.cancelAllJobsAndFlushAsync(() -> Platform.runLater(() -> {
						getCurrentTask().done(Translation.DIALOG_PROGRESS_DONE.toString());
						close();
					})),
					this::closeWhenCurrentTaskFinishes
			).run();
		});
	}

	static Runnable cancellationAction(CancellationScope scope, Runnable globalCancellation, Runnable currentTaskCancellation) {
		return scope == CancellationScope.GLOBAL_JOBS ? globalCancellation : currentTaskCancellation;
	}

	private void closeWhenCurrentTaskFinishes() {
		getCurrentTask().stateProperty().addListener((observable, oldState, newState) -> {
			if (isTerminal(newState)) {
				close();
			}
		});
		if (isTerminal(getCurrentTask().getState())) {
			close();
		}
	}

	private boolean isTerminal(Worker.State state) {
		return state == Worker.State.SUCCEEDED || state == Worker.State.CANCELLED || state == Worker.State.FAILED;
	}

	public boolean cancelled() {
		return cancelled;
	}
}
