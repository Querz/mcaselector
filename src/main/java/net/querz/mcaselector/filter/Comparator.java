package net.querz.mcaselector.filter;

public enum Comparator {

	EQ("=="),
	NEQ("!="),
	ST("<"),
	LT(">"),
	LEQ(">="),
	SEQ("<="),
	CONTAINS("\u2286"),
	CONTAINS_NOT("!\u2286");

	private String string;

	Comparator(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}
}
