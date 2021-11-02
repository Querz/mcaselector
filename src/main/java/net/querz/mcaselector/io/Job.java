package net.querz.mcaselector.io;

public abstract class Job implements Runnable {

	private final RegionDirectories rd;
	private final int priority;

	public static final int PRIORITY_LOW = 20_000_000;
	public static final int PRIORITY_MEDIUM = 10_000_000;
	public static final int PRIORITY_HIGH = 0;

	public Job(RegionDirectories rd, int priority) {
		this.rd = rd;
		this.priority = priority;
	}

	public RegionDirectories getRegionDirectories() {
		return rd;
	}

	// can be overwritten by individual jobs when something has to be done when this job is cancelled
	public void cancel() {}

	private boolean done;

	public void done() {
		this.done = true;
	}

	public boolean isDone() {
		return done;
	}

	public int getPriority() {
		return priority;
	}

	public int getBasePriority() {
		return priority;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + rd.getLocation();
	}
}