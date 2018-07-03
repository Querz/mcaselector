package net.querz.mcaselector.filter.structure;

import net.querz.mcaselector.util.Helper;
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

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (!isValid()) {
			try {
				value = Helper.parseDuration(raw) * 20;
				valid = true;
			} catch (IllegalArgumentException ex) {
				value = 0;
				valid = false;
			}
		}
	}

	@Override
	public String getFormatText() {
		return "duration";
	}
}
