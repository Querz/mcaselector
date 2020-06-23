package net.querz.mcaselector.io;

import java.io.File;

public abstract class Job implements Runnable {

	private final File file;

	public Job(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}
}