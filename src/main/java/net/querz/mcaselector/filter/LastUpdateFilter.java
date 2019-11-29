package net.querz.mcaselector.filter;

import net.querz.mcaselector.text.TextHelper;

public class LastUpdateFilter extends IntegerFilter {

	public LastUpdateFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	private LastUpdateFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.LAST_UPDATE, operator, comparator, value);
	}

	@Override
	Integer getNumber(FilterData data) {
		return data.getLastUpdated();
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (!isValid()) {
			try {
				setFilterNumber(TextHelper.parseTimestamp(raw));
				setValid(true);
				setRawValue(raw);
			} catch (IllegalArgumentException ex) {
				setFilterNumber(0);
				setValid(false);
			}
		}
	}

	@Override
	public String getFormatText() {
		return "YYYY-MM-DD hh:mm:ss";
	}

	@Override
	public String toString() {
		return "LastUpdate " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public LastUpdateFilter clone() {
		return new LastUpdateFilter(getOperator(), getComparator(), value);
	}
}
