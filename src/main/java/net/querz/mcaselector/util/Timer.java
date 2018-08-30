package net.querz.mcaselector.util;

public class Timer {

	private long start = System.nanoTime();

	public long getMillis() {
		return getNano() / 1_000_000;
	}

	public long getNano() {
		return System.nanoTime() - start;
	}

	public void reset() {
		start = System.nanoTime();
	}

	@Override
	public String toString() {
		return getMillis() + "ms";
	}
}
