package net.querz.mcaselector.filter;

public abstract class Condition extends Filter {

	private Operator operator = Operator.AND;
	protected Comparator comparator;

	public Condition(Comparator comparator) {
		this.comparator = comparator;
	}

	public Condition(Operator operator, Comparator comparator) {
		this.operator = operator;
		this.comparator = comparator;
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

	@Override
	public Comparator getComparator() {
		return comparator;
	}

	@Override
	public boolean matches(FilterData data) {
		switch (comparator) {
			case LARGER_THAN:
				return isLarger(data);
			case SMALLER_THAN:
				return isSmaller(data);
			default:
				return isEqual(data);
		}
	}

	public abstract boolean isLarger(FilterData data);

	public abstract boolean isSmaller(FilterData data);

	public abstract boolean isEqual(FilterData data);
}
