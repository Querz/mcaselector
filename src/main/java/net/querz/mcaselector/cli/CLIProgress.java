package net.querz.mcaselector.cli;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import net.querz.mcaselector.progress.Progress;

public class CLIProgress implements Progress {

	private final ProgressBar progressBar;
	private boolean cancelled = false;
	private Runnable doneAction;

	public CLIProgress(String name) {
		ProgressBarBuilder builder = new ProgressBarBuilder()
			.setStyle(ProgressBarStyle.ASCII)
			.setTaskName(name)
			.setUpdateIntervalMillis(200);
		progressBar = builder.build();
	}

	@Override
	public void setMax(int max) {
		progressBar.maxHint(max);
		progressBar.reset();
	}

	@Override
	public void updateProgress(String msg, int progress) {
		progressBar.stepTo(progress);
		progressBar.setExtraMessage(msg);
		checkDone();
	}

	@Override
	public void done(String msg) {
		progressBar.close();
		if (doneAction != null) {
			doneAction.run();
		}
	}

	@Override
	public boolean taskCancelled() {
		return cancelled;
	}

	@Override
	public void cancelTask() {
		cancelled = true;
		progressBar.setExtraMessage("cancelled");
		progressBar.close();
	}

	@Override
	public void incrementProgress(String msg) {
		progressBar.stepBy(1);
		progressBar.setExtraMessage(msg);
		checkDone();
	}

	@Override
	public void incrementProgress(String msg, int progress) {
		progressBar.stepBy(progress);
		progressBar.setExtraMessage(msg);
		checkDone();
	}

	@Override
	public void setMessage(String msg) {
		progressBar.setExtraMessage(msg);
	}

	public void onDone(Runnable doneAction) {
		this.doneAction = doneAction;
	}

	private void checkDone() {
		if (progressBar.getCurrent() >= progressBar.getMax()) {
			progressBar.refresh();
			if (doneAction != null) {
				doneAction.run();
			}
			progressBar.close();
		}
	}
}
