package net.querz.mcaselector.tiles;

import net.querz.mcaselector.io.mca.ChunkData;

public class InhabitedTimeParser implements OverlayDataParser {

	@Override
	public long parseValue(ChunkData chunkData) {
		return chunkData.getRegion().getData().getCompoundTag("Level").getLong("InhabitedTime");
	}

	@Override
	public String name() {
		return "inhabited_time";
	}
}
