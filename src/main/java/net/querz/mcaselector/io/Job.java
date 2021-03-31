package net.querz.mcaselector.io;

public abstract class Job implements Runnable {

	private final RegionDirectories rd;

	public Job(RegionDirectories rd) {
		this.rd = rd;
	}

	public RegionDirectories getRegionDirectories() {
		return rd;
	}

	// can be overwritten by individual jobs when something has to be done when this job is cancelled
	public void cancel() {}
}