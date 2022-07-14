package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountParser;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.ListTag;

public class TileEntityAmountOverlay extends AmountParser {

	public TileEntityAmountOverlay() {
		super(OverlayType.TILE_ENTITY_AMOUNT);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.region() == null) {
			return 0;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(chunkData.getDataVersion());
		ListTag tileEntities = chunkFilter.getTileEntities(chunkData.region().getData());
		return tileEntities == null ? 0 : tileEntities.size();
	}

	@Override
	public String name() {
		return "TileEntityAmount";
	}
}
