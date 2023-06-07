package net.querz.mcaselector.version.anvil118;

import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.anvil117.Anvil117HeightmapCalculator;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import java.util.function.Predicate;

public class Anvil118HeightmapCalculator extends Anvil117HeightmapCalculator {

	@Override
	protected void setHeightMap(CompoundTag root, String name, long[] heightmap) {
		if (heightmap == null) {
			return;
		}
		int dataVersion = root.getIntOrDefault("DataVersion", 0);
		CompoundTag heightmaps = LegacyHelper.getHeightmaps(root, dataVersion);
		if (heightmaps == null) {
			heightmaps = new CompoundTag();
		}
		heightmaps.putLongArray(name, heightmap);
		LegacyHelper.setHeightmaps(root, heightmaps, dataVersion);
	}

	@Override
	protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
		Integer dataVersion = Helper.intFromCompound(root, "DataVersion");
		if (dataVersion == null) {
			return null;
		}

		ListTag sections = LegacyHelper.getSections(root, dataVersion);
		if (sections == null) {
			return new long[37];
		}

		ListTag[] palettes = new ListTag[24];
		long[][] blockStatesArray = new long[24][];
		for (CompoundTag s : sections.iterateType(CompoundTag.class)) {
			ListTag p = LegacyHelper.getPalette(s, dataVersion);
			long[] b = LegacyHelper.getBlockStates(s, dataVersion);
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y < 20 && p != null && b != null) {
				palettes[y + 4] = p;
				blockStatesArray[y + 4] = b;
			}
		}

		short[] heightmap = new short[256];

		// loop over x/z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			loop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				for (int i = 23; i >= 0; i--) {
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
}
