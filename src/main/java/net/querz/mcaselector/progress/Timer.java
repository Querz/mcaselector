package net.querz.mcaselector.progress;

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
		return formatNano(getNano());
	}

	public static String formatNano(long nano) {
		return String.format("%d.%06dms", nano / 1_000_000, nano % 1_000_000);
	}
}
