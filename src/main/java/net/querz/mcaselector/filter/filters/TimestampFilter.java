package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.IntFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;

public class TimestampFilter extends IntFilter {

	public TimestampFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	private TimestampFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.TIMESTAMP, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		return data.region().getTimestamp();
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (!isValid()) {
			try {
				setFilterNumber(TextHelper.parseTimestamp(raw));
				setValid(true);
				setRawValue(raw);
			} catch (IllegalArgumentException ex) {
				setFilterNumber(0);
				setValid(false);
			}
		}
	}

	@Override
	public String getFormatText() {
		return "YYYY-MM-DD hh:mm:ss";
	}

	@Override
	public String toString() {
		return "LastUpdate " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public TimestampFilter clone() {
		return new TimestampFilter(getOperator(), getComparator(), value);
	}
}
