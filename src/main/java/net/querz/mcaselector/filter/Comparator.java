package net.querz.mcaselector.filter;

public enum Comparator {

	EQ("=="),
	NEQ("!="),
	ST("<"),
	LT(">"),
	LEQ(">="),
	SEQ("<="),
	CONTAINS("contains");

	private String string;

	Comparator(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}
}
