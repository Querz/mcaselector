package net.querz.mcaselector.io;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.progress.Timer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class LoadDataJob extends Job {

	public LoadDataJob(File file) {
		super(file);
	}

	public byte[] load() {
		return load(getFile());
	}

	public byte[] load(int length) {
		return load(getFile(), length);
	}

	protected byte[] load(File file) {
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
			Debug.error(ex);
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