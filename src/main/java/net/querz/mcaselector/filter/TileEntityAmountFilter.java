package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

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
		Tag<?> rawTileEntities = data.getRegion().getData().getCompoundTag("Level").get("TileEntities");
		if (rawTileEntities == null || rawTileEntities.getID() == LongArrayTag.ID) {
			return 0;
		}
		return ((ListTag<?>) rawTileEntities).asCompoundTagList().size();
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