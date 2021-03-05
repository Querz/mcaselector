package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;

public interface OverlayDataParser {

	long parseValue(ChunkData chunkData);

	String name();
}
