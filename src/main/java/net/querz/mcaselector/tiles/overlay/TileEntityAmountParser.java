package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

public class TileEntityAmountParser extends AmountParser {

	public TileEntityAmountParser() {
		super(OverlayType.TILE_ENTITY_AMOUNT);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.getRegion() == null || chunkData.getRegion().getData() == null) {
			return 0;
		}
		Tag<?> rawTileEntities = chunkData.getRegion().getData().getCompoundTag("Level").get("TileEntities");
		if (rawTileEntities == null || rawTileEntities.getID() == LongArrayTag.ID) {
			return 0;
		}
		return ((ListTag<?>) rawTileEntities).asCompoundTagList().size();
	}

	@Override
	public String name() {
		return "TileEntityAmount";
	}
}
