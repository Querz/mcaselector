package net.querz.mcaselector.filter.structure;

import net.querz.mcaselector.util.Helper;

public class LastUpdateFilter extends IntegerFilter {

	public LastUpdateFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	public LastUpdateFilter(Operator operator, Comparator comparator, int value) {
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
				value = Helper.parseTimestamp(raw);
				valid = true;
			} catch (IllegalArgumentException ex) {
				value = 0;
				valid = false;
			}
		}
	}
}
