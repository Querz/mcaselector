package net.querz.mcaselector.filter.structure;

import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import net.querz.nbt.CompoundTag;

public class ZPosFilter extends IntegerFilter {

	public ZPosFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	public ZPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.Z_POS, operator, comparator, value);
	}

	@Override
	Integer getNumber(FilterData data) {
		return ((CompoundTag) data.getChunk().get("Level")).getInt("zPos");
	}

	public boolean matchesRegion(Point2i region) {
		int value = (getFilterNumber() >> 5) << 5;
		return matches(value, Helper.regionToChunk(region).getX());
	}
}
