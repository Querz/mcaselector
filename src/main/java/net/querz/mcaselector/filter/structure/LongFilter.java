package net.querz.mcaselector.filter.structure;

public abstract class LongFilter extends NumberFilter<Long> {

	protected long value;

	public LongFilter(FilterType type, Operator operator, Comparator comparator, long value) {
		super(type, operator, comparator);
		this.value = value;
	}

	@Override
	Long getFilterNumber() {
		return value;
	}

	@Override
	public void setFilterValue(String raw) {
		if (raw == null) {
			value = 0;
			valid = false;
		} else {
			try {
				value = Long.parseLong(raw);
				valid = true;
			} catch (NumberFormatException ex) {
				value = 0;
				valid = false;
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
