package net.querz.mcaselector.filter.structure;

import net.querz.nbt.CompoundTag;

public class InhabitedTimeFilter extends LongFilter {

	public InhabitedTimeFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	public InhabitedTimeFilter(Operator operator, Comparator comparator, long value) {
		super(FilterType.INHABITED_TIME, operator, comparator, value);
	}

	@Override
	Long getNumber(FilterData data) {
		return ((CompoundTag) data.getChunk().get("Level")).getLong("InhabitedTime");
	}
}
