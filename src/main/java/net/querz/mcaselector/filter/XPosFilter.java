package net.querz.mcaselector.filter;

import net.querz.mcaselector.util.Point2i;
import net.querz.nbt.CompoundTag;

public class XPosFilter extends IntegerFilter {

	public XPosFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	public XPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.X_POS, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(FilterData data) {
		return ((CompoundTag) data.getChunk().get("Level")).getInt("xPos");
	}

	public boolean matchesRegion(Point2i region) {
		Point2i chunk = region.regionToChunk();
		for (int i = 0; i < 32; i++) {
			Point2i p = chunk.add(i);
			if (matches(getFilterNumber(), p.getX())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public XPosFilter clone() {
		return new XPosFilter(getOperator(), getComparator(), value);
	}
}
