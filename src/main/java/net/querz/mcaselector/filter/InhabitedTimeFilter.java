package net.querz.mcaselector.filter;

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
	protected Long getNumber(FilterData data) {
		return ((CompoundTag) data.getChunk().get("Level")).getLong("InhabitedTime");
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (!isValid()) {
			try {
				//InhabitedTime is in ticks, not seconds
				setFilterNumber(Helper.parseDuration(raw) * 20);
				setValid(true);
			} catch (IllegalArgumentException ex) {
				setFilterNumber(0L);
				setValid(false);
			}
		}
	}

	@Override
	public String getFormatText() {
		return "duration";
	}

	@Override
	public InhabitedTimeFilter clone() {
		return new InhabitedTimeFilter(getOperator(), getComparator(), value);
	}
}
