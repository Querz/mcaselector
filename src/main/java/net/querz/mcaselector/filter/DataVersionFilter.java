package net.querz.mcaselector.filter;

public class DataVersionFilter extends IntegerFilter {

	public DataVersionFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.DATA_VERSION, operator, comparator, value);
	}

	@Override
	Integer getNumber(FilterData data) {
		return data.getChunk().getInt("DataVersion");
	}
}
