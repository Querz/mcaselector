package net.querz.mcaselector.io2;

import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Timer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class LoadDataJob extends Job<File> {

	public LoadDataJob(File file) {
		super(file);
	}

	public byte[] loadBytes() {
		Timer t = new Timer();
		long length = get().length();
		if (length > 0) {
			int read;
			byte[] data = new byte[(int) length];
			try (FileInputStream fis = new FileInputStream(get())) {
				read = fis.read(data);
			} catch (IOException ex) {
				ex.printStackTrace();
				return null;
			}
			Debug.dumpf("read %d bytes from %s in %s", read, get().getAbsolutePath(), t);
			return data;
		}
		return null;
	}

	public byte[] loadBytes(int length) {
		Timer t = new Timer();
		int read;
		byte[] data = new byte[length];
		try (FileInputStream fis = new FileInputStream(get())) {
			read = fis.read(data);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		Debug.dumpf("read %d bytes from %s in %s", read, get().getAbsolutePath(), t);
		return data;
	}



	@Override
	public void run() {
		execute();
		//TODO: do some more stuff here
	}

	public abstract void execute();
}