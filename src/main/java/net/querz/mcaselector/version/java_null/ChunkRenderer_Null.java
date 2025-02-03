package net.querz.mcaselector.version.java_null;

import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.CompoundTag;

@MCVersionImplementation(0)
public class ChunkRenderer_Null implements ChunkRenderer<Void, Void> {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping<Void, Void> colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height) {}

	@Override
	public void drawLayer(CompoundTag root, ColorMapping<Void, Void> colorMapping, int x, int z, int scale, int[] pixelBuffer, int height) {}

	@Override
	public void drawCaves(CompoundTag root, ColorMapping<Void, Void> colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height) {}

	@Override
	public CompoundTag minimizeChunk(CompoundTag root) {
		return null;
	}
}
