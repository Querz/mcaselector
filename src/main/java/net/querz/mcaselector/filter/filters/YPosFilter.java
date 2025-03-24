package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.IntFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.IntTag;

public class YPosFilter extends IntFilter {

	public YPosFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	public YPosFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.Y_POS, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		IntTag tag = VersionHandler.getImpl(data, ChunkFilter.Pos.class).getYPos(data);
		return tag == null ? null : tag.asInt();
	}

	@Override
	public YPosFilter clone() {
		return new YPosFilter(getOperator(), getComparator(), value);
	}
}
