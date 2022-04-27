package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.ListTag;

public class TileEntityAmountParser extends AmountParser {

	public TileEntityAmountParser() {
		super(OverlayType.TILE_ENTITY_AMOUNT);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.region() == null) {
			return 0;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(chunkData.region().getData().getInt("DataVersion"));
		ListTag tileEntities = chunkFilter.getTileEntities(chunkData.region().getData());
		return tileEntities == null ? 0 : tileEntities.size();
	}

	@Override
	public String name() {
		return "TileEntityAmount";
	}
}
