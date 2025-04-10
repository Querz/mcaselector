package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.IntFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.ListTag;

public class TileEntityAmountFilter extends IntFilter {

	public TileEntityAmountFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	private TileEntityAmountFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.TILE_ENTITY_AMOUNT, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		ListTag tileEntities = VersionHandler.getImpl(data, ChunkFilter.TileEntities.class).getTileEntities(data);
		return tileEntities == null ? 0 : tileEntities.size();
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (isValid() && getFilterValue() < 0) {
			setFilterNumber(0);
			setValid(false);
		}
	}

	@Override
	public TileEntityAmountFilter clone() {
		return new TileEntityAmountFilter(getOperator(), getComparator(), value);
	}
}