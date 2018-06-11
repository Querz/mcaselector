package net.querz.mcaselector.filter;

public enum Comparator {

	LARGER_THAN(">"),
	SMALLER_THAN("<"),
	EQUAL("==");

	private String string;

	Comparator(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}
}
