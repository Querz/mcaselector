package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;

public class InhabitedTimeParser extends OverlayDataParser {

	@Override
	public int parseValue(ChunkData chunkData) {
		return ValidationHelper.withDefaultSilent(() -> chunkData.getRegion().getData().getCompoundTag("Level").getNumber("InhabitedTime").intValue(), 0);
	}

	@Override
	public String name() {
		return "InhabitedTime";
	}
}
