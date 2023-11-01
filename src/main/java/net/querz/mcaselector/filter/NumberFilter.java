package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;

public abstract class NumberFilter<T extends Number> extends Filter<T> {

	private static final Comparator[] comparators = new Comparator[] {
		Comparator.EQUAL,
		Comparator.NOT_EQUAL,
		Comparator.LARGER,
		Comparator.SMALLER,
		Comparator.LARGER_EQUAL,
		Comparator.SMALLER_EQUAL
	};

	private Comparator comparator;

	public NumberFilter(FilterType type, Operator operator, Comparator comparator) {
		super(type, operator);
		this.comparator = comparator;
	}

	@Override
	public T getFilterValue() {
		return getFilterNumber();
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

	public boolean matches(T value, T data, Comparator comparator) {
		return switch (comparator) {
			case EQUAL -> isEqual(data, value);
			case NOT_EQUAL -> isNotEqual(data, value);
			case LARGER -> isLargerThan(data, value);
			case SMALLER -> isSmallerThan(data, value);
			case LARGER_EQUAL -> isLargerEqual(data, value);
			case SMALLER_EQUAL -> isSmallerEqual(data, value);
			default -> false;
		};
	}

	@Override
	public boolean matches(ChunkData data) {
		return matches(getFilterNumber(), getNumber(data), comparator);
	}

	@Override
	public String toString() {
		return getType() + " " + comparator.getQueryString() + " " + getFilterValue();
	}

	public abstract String getFormatText();

	protected abstract T getFilterNumber();

	protected abstract void setFilterNumber(T value);

	protected abstract T getNumber(ChunkData data);

	protected abstract boolean isEqual(T a, T b);

	protected abstract boolean isNotEqual(T a, T b);

	protected abstract boolean isLargerThan(T a, T b);

	protected abstract boolean isSmallerThan(T a, T b);

	protected abstract boolean isLargerEqual(T a, T b);

	protected abstract boolean isSmallerEqual(T a, T b);
}
