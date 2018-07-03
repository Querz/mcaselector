package net.querz.mcaselector.filter.structure;

import java.io.Serializable;

public abstract class Filter<T> implements Serializable {

	private String rawValue;
	protected boolean valid = true;
	private FilterType type;
	private Operator operator;
	private Filter parent;

	public Filter(FilterType type) {
		this(type, Operator.AND);
	}

	public Filter(FilterType type, Operator operator) {
		this.type = type;
		this.operator = operator;
	}

	public boolean isValid() {
		return valid;
	}

	public String getRawValue() {
		return rawValue;
	}

	public void setRawValue(String rawValue) {
		this.rawValue = rawValue;
	}

	public Operator getOperator() {
		return operator;
	}

	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	public FilterType getType() {
		return type;
	}

	public void setParent(Filter parent) {
		this.parent = parent;
	}

	public Filter getParent() {
		return parent;
	}

	public abstract T getFilterValue();

	public abstract void setFilterValue(String raw);

	public abstract Comparator[] getComparators();

	public abstract Comparator getComparator();

	public abstract boolean matches(FilterData data);

	public abstract String toString(FilterData data);
}
