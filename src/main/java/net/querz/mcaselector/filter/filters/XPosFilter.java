package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.IntTag;

public class XPosFilter extends IntFilter implements RegionMatcher {

	public XPosFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public XPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.X_POS, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		IntTag tag = VersionHandler.getImpl(data, ChunkFilter.Pos.class).getXPos(data);
		return tag == null ? null : tag.asInt();
	}

	@Override
	public MatchType matchesRegion(Point2i region) {
		if (region.equals(new Point2i(19, 2))) {
			System.out.println("test");
		}
		int x = region.regionToChunk().getX();
		int matchCount = 0;
		for (int i = 0; i < 32; i++) {
			if (matches(getFilterNumber(), x + i, getComparator())) {
				matchCount++;
			}
		}
		return matchCount == 0 ? MatchType.NONE : matchCount == 32 ? MatchType.FULL : MatchType.PARTIAL;
	}

	@Override
	public XPosFilter clone() {
		return new XPosFilter(getOperator(), getComparator(), value);
	}
}
