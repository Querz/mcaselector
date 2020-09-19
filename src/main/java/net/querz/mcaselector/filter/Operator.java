package net.querz.mcaselector.filter;

public enum Operator {

	AND("AND"),
	OR("OR");

	private final String string;

	Operator(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}

	public static Operator getByName(String name) {
		for (Operator o : Operator.values()) {
			if (o.string.equals(name)) {
				return o;
			}
		}
		return null;
	}

	public static Operator negate(Operator o) {
		switch (o) {
			case AND: return OR;
			case OR: return AND;
		}
		throw new IllegalArgumentException("failed to negate operator " + o);
	}
}
