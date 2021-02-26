package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;

public class DataVersionFilter extends IntFilter {

	public DataVersionFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public DataVersionFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.DATA_VERSION, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		if (data.getRegion() == null) {
			return 0;
		}
		return data.getRegion().getData().getInt("DataVersion");
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
