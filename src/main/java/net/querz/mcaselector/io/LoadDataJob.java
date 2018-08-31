package net.querz.mcaselector.io;

import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Timer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class LoadDataJob extends Job {

	public LoadDataJob(File file) {
		super(file);
	}

	public byte[] load() {
		Timer t = new Timer();
		long length = getFile().length();
		if (length > 0) {
			int read;
			byte[] data = new byte[(int) length];
			try (FileInputStream fis = new FileInputStream(getFile())) {
				read = fis.read(data);
			} catch (IOException ex) {
				ex.printStackTrace();
				return null;
			}
			Debug.dumpf("read %d bytes from %s in %s", read, getFile().getAbsolutePath(), t);
			return data;
		}
		return null;
	}

	public byte[] load(int length) {
		Timer t = new Timer();
		int read;
		byte[] data = new byte[length];
		try (FileInputStream fis = new FileInputStream(getFile())) {
			read = fis.read(data);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		Debug.dumpf("read %d bytes from %s in %s", read, getFile().getAbsolutePath(), t);
		return data;
	}

	@Override
	public void run() {
		execute();
		MCAFilePipe.refillDataLoadExecutorQueue();
	}

	public abstract void execute();
}