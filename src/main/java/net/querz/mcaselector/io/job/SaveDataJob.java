package net.querz.mcaselector.io.job;

import net.querz.mcaselector.io.Job;
import net.querz.mcaselector.io.RegionDirectories;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;

public abstract class SaveDataJob<T> extends Job {

	private static final Logger LOGGER = LogManager.getLogger(SaveDataJob.class);

	private final T data;

	protected Consumer<Throwable> errorHandler;

	public SaveDataJob(RegionDirectories dirs, T data) {
		super(dirs, PRIORITY_LOW);
		this.data = data;
	}

	public T getData() {
		return data;
	}

	@Override
	public void run() {
		try {
			execute();
		} catch (Throwable t) {
			LOGGER.error("unhandled exception in SaveDataJob", t);
			if (errorHandler != null) {
				errorHandler.accept(t);
			}
		}
	}

	public abstract void execute();

	// can be overwritten to indicate that this job can be skipped in favor for load and process jobs
	public boolean canSkip() {
		return false;
	}
}