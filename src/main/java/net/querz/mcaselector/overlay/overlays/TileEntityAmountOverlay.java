package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountOverlay;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.ListTag;

public class TileEntityAmountOverlay extends AmountOverlay {

	public TileEntityAmountOverlay() {
		super(OverlayType.TILE_ENTITY_AMOUNT, 0, Integer.MAX_VALUE);
	}

	@Override
	public int parseValue(ChunkData data) {
		ListTag tileEntities = VersionHandler.getImpl(data, ChunkFilter.TileEntities.class).getTileEntities(data);
		return tileEntities == null ? 0 : tileEntities.size();
	}

	@Override
	public String name() {
		return "TileEntityAmount";
	}
}
