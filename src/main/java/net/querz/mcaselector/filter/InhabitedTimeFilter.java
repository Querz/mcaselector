package net.querz.mcaselector.filter;

import net.querz.nbt.CompoundTag;

public class InhabitedTimeFilter extends LongFilter {

	public InhabitedTimeFilter(Operator operator, Comparator comparator, long value) {
		super(FilterType.INHABITED_TIME, operator, comparator, value);
	}

	@Override
	Long getNumber(FilterData data) {
		return ((CompoundTag) data.getChunk().get("Level")).getLong("InhabitedTime");
	}
}
