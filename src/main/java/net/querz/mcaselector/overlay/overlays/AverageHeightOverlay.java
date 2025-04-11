package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountOverlay;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;

public class AverageHeightOverlay extends AmountOverlay {

	public AverageHeightOverlay() {
		super(OverlayType.AVERAGE_HEIGHT);
	}

	@Override
	public int parseValue(ChunkData data) {
		return VersionHandler.getImpl(data, ChunkFilter.Blocks.class).getAverageHeight(data);
	}

	@Override
	public String name() {
		return "AverageHeight";
	}
}
