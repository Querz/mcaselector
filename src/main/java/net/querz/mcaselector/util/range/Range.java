package net.querz.mcaselector.util.range;

import java.util.function.Consumer;

public class Range {

	// from and to are both inclusive
	private int from;
	private int to;

	public Range(int from, int to) {
		this.from = from;
		this.to = to;
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public int num() {
		return to - from + 1;
	}

	public Range limit(Range range) {
		if (from > to) {
			from = range.from;
			to = range.to;
		} else {
			from = Math.max(from, range.from);
			to = Math.min(to, range.to);
		}
		return this;
	}

	public boolean contains(int value) {
		return from <= value && to >= value;
	}

	public boolean isMaxRange() {
		return from == Integer.MIN_VALUE && to == Integer.MAX_VALUE;
	}

	public void forEach(Consumer<Integer> iteration, int min, int max) {
		int m = Math.min(to, max);
		for (int i = Math.max(from, min); i < m; i++) {
			iteration.accept(i);
		}
	}

	@Override
	public String toString() {
		if (from == to) {
			return from + "";
		}
		return (from == Integer.MIN_VALUE ? "" : from) + ":" + (to == Integer.MAX_VALUE ? "" : to);
	}
}
