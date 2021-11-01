package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;

public class ZPosFilter extends IntFilter implements RegionMatcher {

	public ZPosFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public ZPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.Z_POS, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		if (data.getRegion() == null) {
			return null;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		return chunkFilter.getZPos(data.getRegion().getData()).asInt();
	}

	@Override
	public boolean matchesRegion(Point2i region) {
		Point2i chunk = region.regionToChunk();
		for (int i = 0; i < 32; i++) {
			Point2i p = chunk.add(i);
			if (matches(getFilterNumber(), p.getZ())) {
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
