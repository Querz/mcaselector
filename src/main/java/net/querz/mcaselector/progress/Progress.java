package net.querz.mcaselector.progress;

public interface Progress {

	void setMax(int max);

	void updateProgress(String msg, int progress);

	void done(String msg);

	boolean taskCancelled();

	void cancelTask();

	void incrementProgress(String msg);

	void incrementProgress(String msg, int progress);

	void setMessage(String msg);
}
