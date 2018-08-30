package net.querz.mcaselector.io;


import java.io.File;

public abstract class SaveDataJob<T> extends Job {

	private T data;

	public SaveDataJob(File file, T data) {
		super(file);
		this.data = data;
	}

	public T getData() {
		return data;
	}

	@Override
	public void run() {
		MCAFilePipe.refillDataLoadExecutorQueue();
		execute();
	}

	public abstract void execute();
}