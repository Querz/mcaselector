package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.ByteFilter;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.ByteTag;

public class LightPopulatedFilter extends ByteFilter {

	private static final Comparator[] comparators = {
			Comparator.EQUAL,
			Comparator.NOT_EQUAL
	};

	public LightPopulatedFilter() {
		this(Operator.AND, Comparator.EQUAL, (byte) 0);
	}

	private LightPopulatedFilter(Operator operator, Comparator comparator, byte value) {
		super(FilterType.LIGHT_POPULATED, operator, comparator, value);
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	protected Byte getNumber(ChunkData data) {
		if (data.region() == null || data.region().getData() == null) {
			return 0;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		ByteTag tag = chunkFilter.getLightPopulated(data.region().getData());
		return tag == null ? 0 : tag.asByte();
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (isValid() && (getFilterValue() != 1 && getFilterValue() != 0)) {
			setFilterNumber((byte) 0);
			setValid(false);
		}
	}

	@Override
	public String getFormatText() {
		return "1|0";
	}

	@Override
	public LightPopulatedFilter clone() {
		return new LightPopulatedFilter(getOperator(), getComparator(), value);
	}
}
