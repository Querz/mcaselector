package net.querz.mcaselector.version;

import net.querz.nbt.tag.CompoundTag;

public interface ChunkDataProcessor {

	void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, short[] heights, boolean water);
}
