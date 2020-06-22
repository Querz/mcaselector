package net.querz.mcaselector.version;

import net.querz.mcaselector.range.Range;
import net.querz.nbt.tag.CompoundTag;
import java.util.List;
import java.util.Random;

public interface ChunkDataProcessor {

	Random random = new Random();

	void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, int[] waterPixels, byte[] terrainHeights, byte[] waterHeights, boolean water);

	void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges);
}
