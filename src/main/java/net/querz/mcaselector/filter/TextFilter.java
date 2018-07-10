package net.querz.mcaselector.filter;

public abstract class TextFilter<T> extends Filter<T> {

	private static final Comparator[] comparators = {
			Comparator.CONTAINS,
			Comparator.CONTAINS_NOT
	};

	private T value;

	private Comparator comparator;

	public TextFilter(FilterType type, Operator operator, Comparator comparator, T value) {
		super(type, operator);
		this.comparator = comparator;
		this.value = value;
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public Comparator getComparator() {
		return comparator;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	@Override
	public boolean matches(FilterData data) {
		switch (comparator) {
		case CONTAINS:
			return contains(value, data);
		case CONTAINS_NOT:
			return containsNot(value, data);
		}
		return false;
	}

	@Override
	public T getFilterValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public abstract String getFormatText();

	public abstract boolean contains(T value, FilterData data);

	public abstract boolean containsNot(T value, FilterData data);
}
