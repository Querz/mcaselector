package net.querz.mcaselector.filter;

public enum Comparator {

	EQUAL("="),
	LARGER_THAN(">"),
	SMALLER_THAN("<");

	private String string;

	Comparator(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}
}
