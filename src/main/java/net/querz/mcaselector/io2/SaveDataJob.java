package net.querz.mcaselector.io2;

import java.io.File;

public abstract class SaveDataJob<T> extends Job<File> {

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
		execute();
	}

	public abstract void execute();
}