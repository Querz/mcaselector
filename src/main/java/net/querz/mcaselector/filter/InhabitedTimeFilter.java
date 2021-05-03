package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;

public class InhabitedTimeFilter extends LongFilter {

	public InhabitedTimeFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	private InhabitedTimeFilter(Operator operator, Comparator comparator, long value) {
		super(FilterType.INHABITED_TIME, operator, comparator, value);
	}

	@Override
	protected Long getNumber(ChunkData data) {
		if (data.getRegion() == null) {
			return 0L;
		}
		return data.getRegion().getData().getCompoundTag("Level").getLong("InhabitedTime");
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
