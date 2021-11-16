package net.querz.mcaselector.io.job;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.Job;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.progress.Timer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public abstract class ProcessDataJob extends Job {

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
		return load(getRegionDirectories().getPoi(), 8192);
	}

	public byte[] loadEntitiesHeader() {
		return load(getRegionDirectories().getEntities(), 8192);
	}

	public byte[] loadRegionHeader() {
		return load(getRegionDirectories().getRegion(), 8192);
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
			Debug.dumpException("failed to read data from " + file, ex);
			return null;
		}
		Debug.dumpf("read %d bytes from %s in %s", read, file.getAbsolutePath(), t);
		return data;
	}

	@Override
	public void run() {
		if (execute()) {
			done();
		}
	}

	public abstract boolean execute();
}