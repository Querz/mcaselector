package net.querz.mcaselector.filter;

public class LastUpdateFilter extends IntegerFilter {

	public LastUpdateFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.LAST_UPDATE, operator, comparator, value);
	}

	@Override
	Integer getNumber(FilterData data) {
		return data.getLastUpdated();
	}
}
