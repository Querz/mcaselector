package net.querz.mcaselector.filter;

public abstract class NumberFilter<T extends Number> extends Filter<T> {

	private static final Comparator[] comparators = new Comparator[] {
		Comparator.EQ,
		Comparator.NEQ,
		Comparator.LT,
		Comparator.ST,
		Comparator.LEQ,
		Comparator.SEQ
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
		return new Comparator[]{};
	}

	@Override
	public Comparator getComparator() {
		return comparator;
	}

	public boolean matches(T value, T data) {
		switch (comparator) {
			case EQ:
				return isEqual(data, value);
			case NEQ:
				return isNotEqual(data, value);
			case LT:
				return isLargerThan(data, value);
			case ST:
				return isSmallerThan(data, value);
			case LEQ:
				return isLargerEqual(data, value);
			case SEQ:
				return isSmallerEqual(data, value);
		}
		return false;
	}

	@Override
	public boolean matches(FilterData data) {
		return matches(getFilterNumber(), getNumber(data));
	}

	@Override
	public String toString(FilterData data) {
		return getFilterValue() + " " + comparator + " " + getNumber(data);
	}

	abstract T getFilterNumber();

	abstract T getNumber(FilterData data);

	abstract boolean isEqual(T a, T b);

	abstract boolean isNotEqual(T a, T b);

	abstract boolean isLargerThan(T a, T b);

	abstract boolean isSmallerThan(T a, T b);

	abstract boolean isLargerEqual(T a, T b);

	abstract boolean isSmallerEqual(T a, T b);
}
