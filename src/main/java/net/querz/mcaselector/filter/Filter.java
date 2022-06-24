package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;
import java.io.Serializable;

public abstract class Filter<T> implements Serializable {

	private String rawValue;
	private boolean valid = true;
	private final FilterType type;
	private Operator operator;
	private Filter<?> parent;

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

	protected void setValid(boolean valid) {
		this.valid = valid;
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

	public void setParent(Filter<?> parent) {
		this.parent = parent;
	}

	public Filter<?> getParent() {
		return parent;
	}

	public void resetTempData() {}

	public boolean selectionOnly() {
		return false;
	}

	public abstract T getFilterValue();

	public abstract void setFilterValue(String raw);

	public abstract Comparator[] getComparators();

	public abstract Comparator getComparator();

	public abstract void setComparator(Comparator comparator);

	public abstract boolean matches(ChunkData data);

	public abstract Filter<T> clone();

	protected static String escape(String value) {
		if (value == null) {
			return "\"\"";
		}
		return "\"" + value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"") + "\"";
	}
}
