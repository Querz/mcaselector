package net.querz.mcaselector.filter;

import net.querz.mcaselector.point.Point2i;

public class ZPosFilter extends IntFilter implements RegionMatcher {

	public ZPosFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public ZPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.Z_POS, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(FilterData data) {
		return data.getChunk().getCompoundTag("Level").getInt("zPos");
	}

	@Override
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
