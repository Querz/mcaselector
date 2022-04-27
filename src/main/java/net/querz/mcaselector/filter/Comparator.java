package net.querz.mcaselector.filter;

public enum Comparator {

	EQUAL("="),
	NOT_EQUAL("\u2260", "!="),
	SMALLER("<"),
	LARGER(">"),
	LARGER_EQUAL("\u2265", ">="),
	SMALLER_EQUAL("\u2264", "<="),
	CONTAINS("\u2283", "contains"),
	CONTAINS_NOT("\u2285", "!contains"),
	INTERSECTS("\u2229", "intersects");

	private final String string;
	private final String query;

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

	public static Comparator negate(Comparator c) {
		return switch (c) {
			case EQUAL -> NOT_EQUAL;
			case NOT_EQUAL -> EQUAL;
			case SMALLER -> LARGER_EQUAL;
			case LARGER -> SMALLER_EQUAL;
			case SMALLER_EQUAL -> LARGER;
			case LARGER_EQUAL -> SMALLER;
			case CONTAINS, INTERSECTS -> CONTAINS_NOT;
			case CONTAINS_NOT -> CONTAINS;
		};
	}
}
