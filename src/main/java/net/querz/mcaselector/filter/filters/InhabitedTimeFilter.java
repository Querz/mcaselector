package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.LongFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.LongTag;

public class InhabitedTimeFilter extends LongFilter {

	public InhabitedTimeFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public InhabitedTimeFilter(Operator operator, Comparator comparator, long value) {
		super(FilterType.INHABITED_TIME, operator, comparator, value);
	}

	@Override
	protected Long getNumber(ChunkData data) {
		LongTag tag = VersionHandler.getImpl(data, ChunkFilter.InhabitedTime.class).getInhabitedTime(data);
		return tag == null ? 0L : tag.asLong();
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (!isValid()) {
			try {
				// InhabitedTime is in ticks, not seconds
				setFilterNumber(TextHelper.parseDuration(raw) * 20);
				setValid(true);
				setRawValue(raw);
			} catch (IllegalArgumentException ex) {
				setFilterNumber(0L);
				setValid(false);
			}
		}
	}

	@Override
	public String toString() {
		return "InhabitedTime " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
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
