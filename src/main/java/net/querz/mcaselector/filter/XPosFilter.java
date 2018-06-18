package net.querz.mcaselector.filter;

import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import net.querz.nbt.CompoundTag;

public class XPosFilter extends IntegerFilter {

	public XPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.X_POS, operator, comparator, value);
	}

	@Override
	Integer getNumber(FilterData data) {
		return ((CompoundTag) data.getChunk().get("Level")).getInt("xPos");
	}

	public boolean matchesRegion(Point2i region) {
		int value = (getFilterNumber() >> 5) << 5;
		return matches(value, Helper.regionToChunk(region).getX());
	}
}
