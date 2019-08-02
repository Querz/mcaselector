package net.querz.mcaselector.filter;

public enum Comparator {

	EQ("=="),
	NEQ("!="),
	ST("<"),
	LT(">"),
	LEQ(">="),
	SEQ("<="),
	CONTAINS("\u2287", "contains"),
	CONTAINS_NOT("!\u2287", "!contains");

	private String string;
	private String query;

	Comparator(String string) {
		this.string = this.query = string;
	}

	// string is the representation used to display the comparator in UI
	// query is the representation used in headless queries
	Comparator(String string, String query) {
		this.string = string;
		this.query = query;
	}

	@Override
	public String toString() {
		return string;
	}

	public String getQueryString() {
		return query == null ? string : query;
	}

	public static Comparator fromString(String s) {
		for (Comparator c : Comparator.values()) {
			if (c.string.equals(s)) {
				return c;
			}
		}
		return null;
	}

	public static Comparator fromQuery(String s) {
		for (Comparator c : Comparator.values()) {
			if (c.query.equals(s)) {
				return c;
			}
		}
		return null;
	}
}
