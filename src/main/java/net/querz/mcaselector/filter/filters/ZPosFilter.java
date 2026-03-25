package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.IntTag;

public class ZPosFilter extends IntFilter implements RegionMatcher {

	public ZPosFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public ZPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.Z_POS, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		IntTag tag = VersionHandler.getImpl(data, ChunkFilter.Pos.class).getZPos(data);
		return tag == null ? null : tag.asInt();
	}

	@Override
	public MatchType matchesRegion(Point2i region) {
		int z = region.regionToChunk().getZ();
		int matchCount = 0;
		for (int i = 0; i < 32; i++) {
			if (matches(getFilterNumber(), z + i, getComparator())) {
				matchCount++;
			} else {
				return matchCount == 0 ? MatchType.NONE : MatchType.PARTIALLY;
			}
		}
		return MatchType.FULLY;
	}

	@Override
	public ZPosFilter clone() {
		return new ZPosFilter(getOperator(), getComparator(), value);
	}
}
