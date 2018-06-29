package net.querz.mcaselector.filter.structure;

public abstract class IntegerFilter extends NumberFilter<Integer> {

	private int value;

	public IntegerFilter(FilterType type, Operator operator, Comparator comparator, int value) {
		super(type, operator, comparator);
		this.value = value;
	}

	@Override
	Integer getFilterNumber() {
		return value;
	}

	@Override
	public boolean setFilterValue(String raw) {
		if (raw == null || raw.isEmpty() || raw.equals("-") || raw.equals("+")) {
			value = 0;
			return true;
		} else {
			try {
				value = Integer.parseInt(raw);
				return true;
			} catch (NumberFormatException ex) {
				return false;
			}
		}
	}

	@Override
	boolean isEqual(Integer a, Integer b) {
		return a.intValue() == b.intValue();
	}

	@Override
	boolean isNotEqual(Integer a, Integer b) {
		return a.intValue() != b.intValue();
	}

	@Override
	boolean isLargerThan(Integer a, Integer b) {
		return a > b;
	}

	@Override
	boolean isSmallerThan(Integer a, Integer b) {
		return a < b;
	}

	@Override
	boolean isLargerEqual(Integer a, Integer b) {
		return a >= b;
	}

	@Override
	boolean isSmallerEqual(Integer a, Integer b) {
		return a <= b;
	}
}
