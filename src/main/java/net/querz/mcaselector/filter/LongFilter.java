package net.querz.mcaselector.filter;

public abstract class LongFilter extends NumberFilter<Long> {

	private long value;

	public LongFilter(FilterType type, Operator operator, Comparator comparator, long value) {
		super(type, operator, comparator);
		this.value = value;
		setRawValue(value + "");
	}

	@Override
	protected Long getFilterNumber() {
		return value;
	}

	@Override
	protected void setFilterNumber(Long value) {
		this.value = value;
	}

	@Override
	public void setFilterValue(String raw) {
		if (raw == null) {
			setFilterNumber(0L);
			setValid(false);
		} else {
			try {
				setFilterNumber(Long.parseLong(raw));
				setValid(true);
				setRawValue(raw);
			} catch (NumberFormatException ex) {
				setFilterNumber(0L);
				setValid(false);
			}
		}
	}

	@Override
	boolean isEqual(Long a, Long b) {
		return a.intValue() == b.intValue();
	}

	@Override
	boolean isNotEqual(Long a, Long b) {
		return a.intValue() != b.intValue();
	}

	@Override
	boolean isLargerThan(Long a, Long b) {
		return a > b;
	}

	@Override
	boolean isSmallerThan(Long a, Long b) {
		return a < b;
	}

	@Override
	boolean isLargerEqual(Long a, Long b) {
		return a >= b;
	}

	@Override
	boolean isSmallerEqual(Long a, Long b) {
		return a <= b;
	}

	@Override
	public String getFormatText() {
		return "+/-long";
	}
}
