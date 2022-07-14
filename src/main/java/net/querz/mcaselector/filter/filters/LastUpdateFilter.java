package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.LongFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.LongTag;

public class LastUpdateFilter extends LongFilter {

	public LastUpdateFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	private LastUpdateFilter(Operator operator, Comparator comparator, long value) {
		super(FilterType.LAST_UPDATE, operator, comparator, value);
	}

	@Override
	protected Long getNumber(ChunkData data) {
		if (data.region() == null || data.region().getData() == null) {
			return 0L;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		LongTag tag = chunkFilter.getLastUpdate(data.region().getData());
		return tag == null ? 0L : tag.asLong();
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (!isValid()) {
			try {
				// LastUpdate is in ticks, not seconds
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
		return "LastUpdate " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public String getFormatText() {
		return "duration";
	}

	@Override
	public LastUpdateFilter clone() {
		return new LastUpdateFilter(getOperator(), getComparator(), value);
	}
}
