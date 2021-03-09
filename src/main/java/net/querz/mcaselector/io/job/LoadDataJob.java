package net.querz.mcaselector.io.job;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.Job;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.progress.Timer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class LoadDataJob extends Job {

	public LoadDataJob(RegionDirectories rd) {
		super(rd);
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
		Timer t = new Timer();
		int read;
		byte[] data = new byte[length];
		try (FileInputStream fis = new FileInputStream(file)) {
			read = fis.read(data);
		} catch (IOException ex) {
			Debug.dumpException("failed to read data from " + file, ex);
			return null;
		}
		Debug.dumpf("read %d bytes from %s in %s", read, file.getAbsolutePath(), t);
		return data;
	}

	@Override
	public void run() {
		execute();
		MCAFilePipe.refillDataLoadExecutorQueue();
	}

	public abstract void execute();
}