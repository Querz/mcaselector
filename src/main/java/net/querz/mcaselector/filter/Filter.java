package net.querz.mcaselector.filter;

public abstract class Filter {

	public abstract Operator getOperator();

	public abstract Comparator getComparator();

	public abstract boolean matches(FilterData data);
}
