package net.querz.mcaselector.io;

import net.querz.mcaselector.util.Debug;
import java.io.File;

public abstract class ProcessDataJob extends Job {

	private byte[] data;

	public ProcessDataJob(File file, byte[] data) {
		super(file);
		this.data = data;
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public void run() {
		MCAFilePipe.refillDataLoadExecutorQueue();
		execute();
	}

	public abstract void execute();
}