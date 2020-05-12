package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.version.ChunkDataProcessor;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.tiles.Tile;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil113ChunkDataProcessor implements ChunkDataProcessor {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, short[] heights, boolean water) {
		ListTag<CompoundTag> sections = withDefault(() -> root.getCompoundTag("Level").getListTag("Sections").asCompoundTagList(), null);
		if ("empty".equals(withDefault(() -> root.getCompoundTag("Level").getString("Status"), null))) {
			return;
		}
		sections.sort(this::filterSections);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {

				int[] biomes = withDefault(() -> root.getCompoundTag("Level").getIntArray("Biomes"), null);
				int biome = -1;
				if (biomes != null && biomes.length != 0) {
					biome = biomes[getIndex(cx, 0, cz)];
				}

				//loop over sections
				boolean waterDepth = false;
				sLoop: for (int i = 0; i < sections.size(); i++) {
					final int si = i;
					CompoundTag section;
					ListTag<?> rawPalette;
					ListTag<CompoundTag> palette;
					if ((section = sections.get(si)) == null
							|| (rawPalette = section.getListTag("Palette")) == null
							|| (palette = rawPalette.asCompoundTagList()) == null) {
						continue;
					}
					long[] blockStates = withDefault(() -> sections.get(si).getLongArray("BlockStates"), null);
					if (blockStates == null) {
						continue;
					}

					Byte height = withDefault(() -> sections.get(si).getByte("Y"), null);
					if (height == null) {
						continue;
					}

					int sectionHeight = height * 16;

					int bits = blockStates.length / 64;
					int clean = ((int) Math.pow(2, bits) - 1);

					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.get(paletteIndex);

						//ignore bedrock and netherrack until 75
						if (isIgnoredInNether(biome, blockData, sectionHeight + cy)) {
							continue;
						}

						if (!isEmpty(paletteIndex, blockData)) {
							int regionIndex = (z + cz) * Tile.SIZE + (x + cx);
							if (water) {
								if (!waterDepth) {
									pixelBuffer[regionIndex] = colorMapping.getRGB(blockData) | 0xFF000000;
								}
								if (isWater(blockData)) {
									waterDepth = true;
									continue sLoop;
								}
							} else {
								pixelBuffer[regionIndex] = colorMapping.getRGB(blockData) | 0xFF000000;
							}
							heights[regionIndex] = (short) (sectionHeight + cy);
							continue zLoop;
						}
					}
				}
			}
		}
	}

	private boolean isWater(CompoundTag blockData) {
		switch (blockData.getString("Name")) {
			case "minecraft:seagrass":
			case "minecraft:tall_seagrass":
			case "minecraft:kelp":
			case "minecraft:kelp_plant":
			case "minecraft:water":
			case "minecraft:bubble_column":
				return true;
		}
		return false;
	}

	protected boolean isIgnoredInNether(int biome, CompoundTag blockData, int height) {
		// all nether biomes: nether/neter_wastes, soul_sand_valley, crimson_forest, warped_forest, basalt_deltas
		if (biome == 8 || biome == 170 || biome == 171 || biome == 172 || biome == 173) {
			switch (withDefault(() -> blockData.getString("Name"), "")) {
			case "minecraft:bedrock":
			case "minecraft:flowing_lava":
			case "minecraft:lava":
			case "minecraft:netherrack":
			case "minecraft:nether_quartz_ore":
			case "minecraft:basalt":
				return height > 75;
			}
		}
		return false;
	}

	private boolean isEmpty(int paletteIndex, CompoundTag blockData) {
		if (paletteIndex == 0) {
			return true;
		}
		switch (withDefault(() -> blockData.getString("Name"), "")) {
			case "minecraft:air":
			case "minecraft:cave_air":
			case "minecraft:barrier":
			case "minecraft:structure_void":
				return blockData.size() == 1;
		}
		return false;
	}

	private int getIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}

	protected int getPaletteIndex(int index, long[] blockStates, int bits, int clean) {
		double blockStatesIndex = index / (4096D / blockStates.length);

		int longIndex = (int) blockStatesIndex;
		int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);

		if (startBit + bits > 64) {
			//get msb from current long, no need to cleanup manually, just fill with 0
			int previous = (int) (blockStates[longIndex] >>> startBit);

			//cleanup pattern for bits from next long
			int remainingClean = ((int) Math.pow(2, startBit + bits - 64) - 1);

			//get lsb from next long
			int next = ((int) blockStates[longIndex + 1]) & remainingClean;
			return (next << 64 - startBit) + previous;
		} else {
			return (int) (blockStates[longIndex] >> startBit) & clean;
		}
	}

	private int filterSections(CompoundTag sectionA, CompoundTag sectionB) {
		return withDefault(() -> sectionB.getByte("Y"), (byte) -1) - withDefault(() -> sectionA.getByte("Y"), (byte) -1);
	}
}
