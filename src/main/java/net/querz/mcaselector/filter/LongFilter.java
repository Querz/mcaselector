package net.querz.mcaselector.filter;

public abstract class LongFilter extends NumberFilter<Long> {

	protected long value;

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
				setFilterNumber(Long.parseLong(raw.replace(" ", "")));
				setValid(true);
				setRawValue(raw);
			} catch (NumberFormatException ex) {
				setFilterNumber(0L);
				setValid(false);
			}
		}
	}

	@Override
	protected boolean isEqual(Long a, Long b) {
		return a.longValue() == b.longValue();
	}

	@Override
	protected boolean isNotEqual(Long a, Long b) {
		return a.longValue() != b.longValue();
	}

	@Override
	protected boolean isLargerThan(Long a, Long b) {
		return a > b;
	}

	@Override
	protected boolean isSmallerThan(Long a, Long b) {
		return a < b;
	}

	@Override
	protected boolean isLargerEqual(Long a, Long b) {
		return a >= b;
	}

	@Override
	protected boolean isSmallerEqual(Long a, Long b) {
		return a <= b;
	}

	@Override
	public String getFormatText() {
		return "\u00B1long";
	}
}
