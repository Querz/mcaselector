package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;

public class InhabitedTimeParser implements OverlayDataParser {

	@Override
	public long parseValue(ChunkData chunkData) {
		return ValidationHelper.withDefaultSilent(() -> chunkData.getRegion().getData().getCompoundTag("Level").getLong("InhabitedTime"), 0L);
	}

	@Override
	public String name() {
		return "InhabitedTime";
	}
}
