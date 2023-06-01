package net.querz.mcaselector.io.job;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.Job;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.progress.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

public abstract class ProcessDataJob extends Job {

	private static final Logger LOGGER = LogManager.getLogger(ProcessDataJob.class);

	protected Consumer<Throwable> errorHandler;

	public ProcessDataJob(RegionDirectories dirs, int priority) {
		super(dirs, priority);
	}

	public byte[] loadPoi() {
		return load(getRegionDirectories().getPoi());
	}

	public byte[] loadEntities() {
		return load(getRegionDirectories().getEntities());
	}

	public byte[] loadRegion() {
		return load(getRegionDirectories().getRegion());
	}

	public byte[] loadPoiHeader() {
		return load(getRegionDirectories().getPoi(), FileHelper.HEADER_SIZE);
	}

	public byte[] loadEntitiesHeader() {
		return load(getRegionDirectories().getEntities(), FileHelper.HEADER_SIZE);
	}

	public byte[] loadRegionHeader() {
		return load(getRegionDirectories().getRegion(), FileHelper.HEADER_SIZE);
	}

	protected byte[] load(File file) {
		if (file == null) {
			return null;
		}
		long length = file.length();
		if (length > 0) {
			return load(file, (int) length);
		}
		return null;
	}

	protected byte[] load(File file, int length) {
		if (file == null || !file.exists() || file.length() < length) {
			return null;
		}
		Timer t = new Timer();
		int read;
		byte[] data = new byte[length];
		try (InputStream is = Files.newInputStream(file.toPath(), StandardOpenOption.READ)) {
			read = is.read(data);
		} catch (IOException ex) {
			LOGGER.warn("failed to read data from {}", file, ex);
			return null;
		}
		LOGGER.debug("read {} bytes from {} in {}", read, file.getAbsolutePath(), t);
		return data;
	}

	@Override
	public void run() {
		try {
			if (execute()) {
				done();
			}
		} catch (Throwable t) {
			LOGGER.error("unhandled exception in ProcessDataJob", t);
			if (errorHandler != null) {
				errorHandler.accept(t);
			}
		}
	}

	public abstract boolean execute();
}