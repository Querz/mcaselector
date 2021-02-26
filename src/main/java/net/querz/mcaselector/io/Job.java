package net.querz.mcaselector.io;

public abstract class Job implements Runnable {

	private final RegionDirectories rd;

	public Job(RegionDirectories rd) {
		this.rd = rd;
	}

	public RegionDirectories getRegionDirectories() {
		return rd;
	}
}