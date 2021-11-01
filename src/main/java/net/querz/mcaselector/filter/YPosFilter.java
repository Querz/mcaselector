package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;

public class YPosFilter extends IntFilter {

	public YPosFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public YPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.Y_POS, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		if (data.getRegion() == null) {
			return null;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		return chunkFilter.getYPos(data.getRegion().getData()).asInt();
	}

	@Override
	public YPosFilter clone() {
		return new YPosFilter(getOperator(), getComparator(), value);
	}
}
