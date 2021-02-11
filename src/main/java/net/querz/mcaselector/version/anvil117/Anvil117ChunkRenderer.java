package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil117ChunkRenderer implements ChunkRenderer {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, int[] waterPixels, byte[] terrainHeights, byte[] waterHeights, boolean water) {
		CompoundTag level = withDefault(() -> root.getCompoundTag("Level"), null);
		if (level == null) {
			return;
		}

		String status = withDefault(() -> level.getString("Status"), null);
		if (status == null || "empty".equals(status)) {
			return;
		}

		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return;
		}

		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return;
		}

		sections.sort(this::filterSections);

		int[] biomes = withDefault(() -> level.getIntArray("Biomes"), null);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {

				//loop over sections
				boolean waterDepth = false;
				for (int i = 0; i < sections.size(); i++) {
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

						int biome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);

						//ignore bedrock and netherrack until 75
						if (isIgnoredInNether(biome, blockData, sectionHeight + cy)) {
							continue;
						}

						if (!isEmpty(blockData)) {
							int regionIndex = (z + cz) * Tile.SIZE + (x + cx);
							if (water) {
								if (!waterDepth) {
									pixelBuffer[regionIndex] = colorMapping.getRGB(blockData) | 0xFF000000; // water color
									waterHeights[regionIndex] = (byte) (sectionHeight + cy); // height of highest water or terrain block
								}
								if (isWater(blockData)) {
									waterDepth = true;
									continue;
								} else {
									waterPixels[regionIndex] = colorMapping.getRGB(blockData) | 0xFF000000; // color of block at bottom of water
								}
							} else {
								pixelBuffer[regionIndex] = colorMapping.getRGB(blockData) | 0xFF000000;
							}
							terrainHeights[regionIndex] = (byte) (sectionHeight + cy); // height of bottom of water
							continue zLoop;
						}
					}
				}
			}
		}
	}

	private boolean isWater(CompoundTag blockData) {
		switch (blockData.getString("Name")) {
			case "minecraft:water":
			case "minecraft:bubble_column":
				return true;
		}
		return false;
	}

	private boolean isIgnoredInNether(int biome, CompoundTag blockData, int height) {
		// all nether biomes: nether/nether_wastes, soul_sand_valley, crimson_forest, warped_forest, basalt_deltas
		if (biome == 8 || biome == 170 || biome == 171 || biome == 172 || biome == 173) {
			switch (withDefault(() -> blockData.getString("Name"), "")) {
				case "minecraft:bedrock":
				case "minecraft:flowing_lava":
				case "minecraft:lava":
				case "minecraft:netherrack":
				case "minecraft:nether_quartz_ore":
				case "minecraft:basalt":
				case "minecraft:soul_sand":
				case "minecraft:nether_gold_ore":
				case "minecraft:netherite_block":
				case "minecraft:ancient_debris":
				case "minecraft:crimson_nylium":
				case "minecraft:warped_nylium":
					return height > 75;
			}
		}
		return false;
	}

	private boolean isEmpty(CompoundTag blockData) {
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

	private int getBiomeIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE + z * 4 + x;
	}

	private int getBiomeAtBlock(int[] biomes, int biomeX, int biomeY, int biomeZ) {
		if (biomes == null) {
			return -1;
		}
		if (biomes.length == 1536) {
			biomeY += 64; // adjust for negative y block coordinates
		} else if (biomes.length != 1024) { // still support 256 height
			return -1;
		}
		return biomes[getBiomeIndex(biomeX / 4, biomeY / 4, biomeZ / 4)];
	}

	private int getPaletteIndex(int index, long[] blockStates, int bits, int clean) {
		int indicesPerLong = (int) (64D / bits);
		int blockStatesIndex = index / indicesPerLong;
		int startBit = (index % indicesPerLong) * bits;
		return (int) (blockStates[blockStatesIndex] >> startBit) & clean;
	}

	private int filterSections(CompoundTag sectionA, CompoundTag sectionB) {
		return withDefault(() -> sectionB.getByte("Y"), (byte) -5) - withDefault(() -> sectionA.getByte("Y"), (byte) -5);
	}
}
