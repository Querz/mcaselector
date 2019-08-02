package net.querz.mcaselector.util;

public interface Progress {

	void setMax(int max);

	void updateProgress(String msg, int progress);

	void done(String msg);

	void incrementProgress(String msg);

	void incrementProgress(String msg, int progress);

	void setMessage(String msg);
}
