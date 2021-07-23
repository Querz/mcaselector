package net.querz.mcaselector.headless;

import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.progress.Progress;
import java.util.concurrent.atomic.AtomicInteger;

public class ConsoleProgress implements Progress {

	private int max;
	private final AtomicInteger progress = new AtomicInteger(0);
	private Runnable doneAction;

	public ConsoleProgress() {
		JobHandler.setTrimSaveData(false);
	}

	@Override
	public void cancelTask() {}

	@Override
	public boolean taskCancelled() {
		return false;
	}

	@Override
	public void setMax(int max) {
		this.max = max;
	}

	@Override
	public void updateProgress(String msg, int progress) {
		this.progress.set(progress);
		printProgress(msg);
		if (this.progress.get() >= max) {
			doneAction.run();
		}
	}

	@Override
	public void done(String msg) {
		max = 1;
		updateProgress(msg, 1);
	}

	@Override
	public void incrementProgress(String msg) {
		incrementProgress(msg, 1);
	}

	@Override
	public void incrementProgress(String msg, int progress) {
		int currentProgress = this.progress.incrementAndGet();
		printProgress(msg);
		if (currentProgress >= max) {
			doneAction.run();
		}
	}

	@Override
	public void setMessage(String msg) {
		System.out.print(msg);
	}

	public void onDone(Runnable doneAction) {
		this.doneAction = doneAction;
	}

	private void printProgress(String msg) {
		System.out.printf("%.2f%%\t%s\n", ((double) progress.get() / max * 100), msg);
	}
}
