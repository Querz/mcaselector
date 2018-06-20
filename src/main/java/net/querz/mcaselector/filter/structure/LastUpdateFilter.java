package net.querz.mcaselector.filter.structure;

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
}
