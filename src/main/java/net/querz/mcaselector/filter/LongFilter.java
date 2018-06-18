package net.querz.mcaselector.filter;

public abstract class LongFilter extends NumberFilter<Long> {

	private long value;

	public LongFilter(FilterType type, Operator operator, Comparator comparator, long value) {
		super(type, operator, comparator);
		this.value = value;
	}

	@Override
	Long getFilterNumber() {
		return value;
	}

	@Override
	public boolean setFilterValue(String raw) {
		if (raw == null || raw.isEmpty()) {
			value = 0;
			return true;
		} else {
			try {
				value = Long.parseLong(raw);
				return true;
			} catch (NumberFormatException ex) {
				return false;
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
}
