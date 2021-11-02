package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

public class TileEntityAmountParser extends AmountParser {

	public TileEntityAmountParser() {
		super(OverlayType.TILE_ENTITY_AMOUNT);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.getRegion() == null) {
			return 0;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(chunkData.getRegion().getData().getInt("DataVersion"));
		ListTag<CompoundTag> tileEntities = chunkFilter.getTileEntities(chunkData.getRegion().getData());
		return tileEntities == null ? 0 : tileEntities.size();
	}

	@Override
	public String name() {
		return "TileEntityAmount";
	}
}
