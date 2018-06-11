package net.querz.mcaselector.filter;

public interface Filter {

	Operator getOperator();

	boolean matches(FilterData data);
}
