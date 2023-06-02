package net.querz.mcaselector.version.anvil116;

import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.anvil113.Anvil113HeightmapCalculator;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import java.util.function.Predicate;

public class Anvil116HeightmapCalculator extends Anvil113HeightmapCalculator {

	@Override
	protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return new long[37];
		}

		ListTag[] palettes = new ListTag[16];
		long[][] blockStatesArray = new long[16][];
		sections.forEach(s -> {
			ListTag p = Helper.tagFromCompound(s, "Palette");
			long[] b = Helper.longArrayFromCompound(s, "BlockStates");
			int y = Helper.numberFromCompound(s, "Y", -1).intValue();
			if (y >= 0 && y <= 15 && p != null && b != null) {
				palettes[y] = p;
				blockStatesArray[y] = b;
			}
		});

		short[] heightmap = new short[256];

		// loop over x/z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			loop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				for (int i = 15; i >= 0; i--) {
					ListTag palette = palettes[i];
					if (palette == null) {
						continue;
					}
					long[] blockStates = blockStatesArray[i];
					for (int cy = 15; cy >= 0; cy--) {
						int blockIndex = cy * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + cz * Tile.CHUNK_SIZE + cx;
						if (matcher.test(getBlockAt(blockIndex, blockStates, palette))) {
							heightmap[cz * Tile.CHUNK_SIZE + cx] = (short) (i * Tile.CHUNK_SIZE + cy + 1);
							continue loop;
						}
					}
				}
			}
		}
		return applyHeightMap(heightmap);
	}

	@Override
	protected long[] applyHeightMap(short[] rawHeightmap) {
		long[] data = new long[37];
		int index = 0;
		for (int i = 0; i < 37; i++) {
			long l = 0L;
			for (int j = 0; j < 7 && index < 256; j++, index++) {
				l += ((long) rawHeightmap[index] << (9 * j));
			}
			data[i] = l;
		}
		return data;
	}

	@Override
	protected int getPaletteIndex(int blockIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
		int indicesPerLong = (int) (64D / bits);
		int blockStatesIndex = blockIndex / indicesPerLong;
		int startBit = (blockIndex % indicesPerLong) * bits;
		return (int) Bits.bitRange(blockStates[blockStatesIndex], startBit, startBit + bits);
	}
}
