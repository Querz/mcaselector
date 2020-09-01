package net.querz.mcaselector.io;

import net.querz.mcaselector.progress.Progress;
import org.junit.Assert;
import java.util.concurrent.atomic.AtomicInteger;

public class TestProgress implements Progress {

	private int max;
	private final AtomicInteger progress = new AtomicInteger(0);
	private final Runnable doneAction;

	public TestProgress(Runnable doneAction, long timeout) {
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(timeout * 1000);
				Assert.fail("progress timed out after " + timeout + " seconds (max=" + max + ", progress=" + progress.get() + ")");
			} catch (InterruptedException e) {
				// do nothing
			}
		});
		t.start();
		this.doneAction = () -> {
			doneAction.run();
			t.interrupt();
		};
	}

	@Override
	public void setMax(int max) {
		this.max = max;
	}

	@Override
	public void updateProgress(String msg, int progress) {
		this.progress.set(progress);
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
		if (currentProgress >= max) {
			doneAction.run();
		}
	}

	@Override
	public void setMessage(String msg) {

	}
}