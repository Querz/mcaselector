package net.querz.mcaselector.version.anvil119;

import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.anvil118.Anvil118HeightmapCalculator;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import java.util.function.Predicate;

public class Anvil119HeightmapCalculator extends Anvil118HeightmapCalculator {

	@Override
	protected void setHeightMap(CompoundTag root, String name, long[] heightmap) {
		CompoundTag heightmaps = root.getCompoundOrDefault("Heightmaps", new CompoundTag());
		heightmaps.putLongArray(name, heightmap);
		root.put("Heightmaps", heightmaps);
	}

	@Override
	protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
		ListTag sections = root.getListTag("sections");
		if (sections == null) {
			return new long[37];
		}

		ListTag[] palettes = new ListTag[24];
		long[][] blockStatesArray = new long[24][];
		sections.forEach(s -> {
			ListTag p = Helper.tagFromCompound(Helper.tagFromCompound(s, "block_states"), "palette");
			long[] b = Helper.longArrayFromCompound(Helper.tagFromCompound(s, "block_states"), "data");
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y < 20 && p != null && b != null) {
				palettes[y + 4] = p;
				blockStatesArray[y + 4] = b;
			}
		});

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
