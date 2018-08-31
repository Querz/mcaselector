package net.querz.mcaselector.filter;

public class DataVersionFilter extends IntegerFilter {

	public DataVersionFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	public DataVersionFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.DATA_VERSION, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(FilterData data) {
		return data.getChunk().getInt("DataVersion");
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (isValid() && getFilterNumber() < 0) {
			setValid(false);
			setFilterNumber(0);
		}
	}

	@Override
	public DataVersionFilter clone() {
		return new DataVersionFilter(getOperator(), getComparator(), value);
	}
}
