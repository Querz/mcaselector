package net.querz.mcaselector.io;

public abstract class SaveDataJob<T> extends Job {

	private final T data;

	public SaveDataJob(RegionDirectories dirs, T data) {
		super(dirs);
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