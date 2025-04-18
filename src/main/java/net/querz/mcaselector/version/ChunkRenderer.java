package net.querz.mcaselector.version;

import net.querz.nbt.CompoundTag;

public interface ChunkRenderer<BLOCK, BIOME> {

	void drawChunk(CompoundTag root, ColorMapping<BLOCK, BIOME> colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height);

	void drawLayer(CompoundTag root, ColorMapping<BLOCK, BIOME> colorMapping, int x, int z, int scale, int[] pixelBuffer, int height);

	void drawCaves(CompoundTag root, ColorMapping<BLOCK, BIOME> colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height);

	CompoundTag minimizeChunk(CompoundTag root);
}
