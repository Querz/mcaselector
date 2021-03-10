package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;

public abstract class OverlayDataParser {

	public abstract int parseValue(ChunkData chunkData);

	public abstract String name();

	// can be overwritten to supply multiple data points for a single overlay
	public String[] multiValues() {
		return null;
	}
}
