package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

public class TileEntityAmountFilter extends IntFilter {

	public TileEntityAmountFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	private TileEntityAmountFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.TILE_ENTITY_AMOUNT, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		if (data.getRegion() == null) {
			return 0;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		ListTag<CompoundTag> tileEntities = chunkFilter.getTileEntities(data.getRegion().getData());
		if (tileEntities == null) {
			return 0;
		}
		return tileEntities.size();
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