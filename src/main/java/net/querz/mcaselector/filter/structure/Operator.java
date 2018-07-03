package net.querz.mcaselector.filter.structure;

public enum Operator {

	AND("AND"),
	OR("OR");

	private String string;

	Operator(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}
}
