package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.IntFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.IntTag;

public class DataVersionFilter extends IntFilter {

	public DataVersionFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public DataVersionFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.DATA_VERSION, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		IntTag tag = Helper.getDataVersionTag(Helper.getRegion(data));
		return tag == null ? null : tag.asInt();
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
