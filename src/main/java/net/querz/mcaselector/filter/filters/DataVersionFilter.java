package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.IntFilter;
import net.querz.mcaselector.filter.Operator;
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
		if (data.region() == null || data.region().getData() == null) {
			return 0;
		}
		return data.getDataVersion();
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
