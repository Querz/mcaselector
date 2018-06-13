package net.querz.mcaselector.filter;

public enum Operator {

	AND("And"),
	OR("Or");

	private String string;

	Operator(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}
}
