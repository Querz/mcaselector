package net.querz.mcaselector.changer;

public class Range {

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

	public boolean contains(int value) {
		return from <= value && to >= value;
	}

	public boolean isMaxRange() {
		return from == Integer.MIN_VALUE && to == Integer.MAX_VALUE;
	}

	@Override
	public String toString() {
		if (from == to) {
			return from + "";
		}
		return (from == Integer.MIN_VALUE ? "" : from) + ":" + (to == Integer.MAX_VALUE ? "" : to);
	}
}
