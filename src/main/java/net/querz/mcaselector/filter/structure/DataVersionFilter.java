package net.querz.mcaselector.filter.structure;

public class DataVersionFilter extends IntegerFilter {

	public DataVersionFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	public DataVersionFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.DATA_VERSION, operator, comparator, value);
	}

	@Override
	Integer getNumber(FilterData data) {
		return data.getChunk().getInt("DataVersion");
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (isValid() && value < 0) {
			valid = false;
			value = 0;
		}
	}
}
