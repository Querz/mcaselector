package net.querz.mcaselector.filter;

import net.querz.mcaselector.point.Point2i;
import net.querz.nbt.CompoundTag;

public class ZPosFilter extends IntegerFilter {

	public ZPosFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	public ZPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.Z_POS, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(FilterData data) {
		return ((CompoundTag) data.getChunk().get("Level")).getInt("zPos");
	}

	public boolean matchesRegion(Point2i region) {
		Point2i chunk = region.regionToChunk();
		for (int i = 0; i < 32; i++) {
			Point2i p = chunk.add(i);
			if (matches(getFilterNumber(), p.getY())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ZPosFilter clone() {
		return new ZPosFilter(getOperator(), getComparator(), value);
	}
}
