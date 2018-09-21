package net.querz.mcaselector.filter;

public abstract class IntegerFilter extends NumberFilter<Integer> {

	protected int value;

	public IntegerFilter(FilterType type, Operator operator, Comparator comparator, int value) {
		super(type, operator, comparator);
		this.value = value;
		setRawValue(value + "");
	}

	@Override
	protected Integer getFilterNumber() {
		return value;
	}

	@Override
	protected void setFilterNumber(Integer value) {
		this.value = value;
	}

	@Override
	public void setFilterValue(String raw) {
		if (raw == null) {
			value = 0;
			setValid(false);
		} else {
			try {
				value = Integer.parseInt(raw);
				setValid(true);
				setRawValue(raw);
			} catch (NumberFormatException ex) {
				value = 0;
				setValid(false);
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

	@Override
	public String getFormatText() {
		return "+/-int";
	}
}
