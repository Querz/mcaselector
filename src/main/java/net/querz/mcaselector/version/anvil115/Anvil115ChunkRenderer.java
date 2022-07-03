package net.querz.mcaselector.version.anvil115;

import net.querz.mcaselector.math.MathUtil;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;

public class Anvil115ChunkRenderer implements ChunkRenderer {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height) {
		ListTag sections = NbtHelper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = NbtHelper.tagFromCompound(root, "Level");

		height = MathUtil.clamp(height, 0, 255);

		ListTag[] palettes = new ListTag[16];
		long[][] blockStatesArray = new long[16][];
		sections.forEach(s -> {
			ListTag p = NbtHelper.tagFromCompound(s, "Palette");
			long[] b = NbtHelper.longArrayFromCompound(s, "BlockStates");
			int y = NbtHelper.numberFromCompound(s, "Y", -1).intValue();
			if (y >= 0 && y <= 15 && p != null && b != null) {
				palettes[y] = p;
				blockStatesArray[y] = b;
			}
		});

		int[] biomes = NbtHelper.intArrayFromCompound(level, "Biomes");

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				//loop over sections
				boolean waterDepth = false;
				for (int i = palettes.length - (16 - (height >> 4)); i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						continue;
					}

					long[] blockStates = blockStatesArray[i];
					ListTag palette = palettes[i];

					int sectionHeight = i * Tile.CHUNK_SIZE;

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);

					int startHeight;
					if (height >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - height % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.getCompound(paletteIndex);

						if (isEmpty(blockData)) {
							continue;
						}

						int biome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);
						biome = MathUtil.clamp(biome, 0, 255);

						int regionIndex = ((z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale));
						if (water) {
							if (!waterDepth) {
								pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome); // water color
								waterHeights[regionIndex] = (short) (sectionHeight + cy); // height of highest water or terrain block
							}
							if (isWater(blockData)) {
								waterDepth = true;
								continue;
							} else if (isWaterlogged(blockData)) {
								pixelBuffer[regionIndex] = colorMapping.getRGB(waterDummy, biome); // water color
								waterPixels[regionIndex] = colorMapping.getRGB(blockData, biome); // color of waterlogged block
								waterHeights[regionIndex] = (short) (sectionHeight + cy);
								terrainHeights[regionIndex] = (short) (sectionHeight + cy - 1); // "height" of bottom of water, which will just be 1 block lower so shading works
								continue zLoop;
							} else {
								waterPixels[regionIndex] = colorMapping.getRGB(blockData, biome); // color of block at bottom of water
							}
						} else {
							pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
						}
						terrainHeights[regionIndex] = (short) (sectionHeight + cy); // height of bottom of water
						continue zLoop;
					}
				}
			}
		}
	}

	@Override
	public void drawLayer(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int height) {
		ListTag sections = NbtHelper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = NbtHelper.tagFromCompound(root, "Level");

		height = MathUtil.clamp(height, 0, 255);

		CompoundTag section = null;
		for (CompoundTag s : sections.iterateType(CompoundTag.TYPE)) {
			int y = NbtHelper.numberFromCompound(s, "Y", -1).intValue();
			if (y == height >> 4) {
				section = s;
				break;
			}
		}
		if (section == null) {
			return;
		}

		ListTag palette = NbtHelper.tagFromCompound(section, "Palette");
		long[] blockStates = NbtHelper.longArrayFromCompound(section, "BlockStates");
		if (blockStates == null || palette == null) {
			return;
		}

		int[] biomes = NbtHelper.intArrayFromCompound(level, "Biomes");

		int cy = height % 16;
		int bits = blockStates.length >> 6;
		int clean = ((int) Math.pow(2, bits) - 1);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {
				int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
				CompoundTag blockData = palette.getCompound(paletteIndex);

				if (isEmpty(blockData)) {
					continue;
				}

				int biome = getBiomeAtBlock(biomes, cx, height, cz);
				biome = MathUtil.clamp(biome, 0, 255);

				int regionIndex = (z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale);
				pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
			}
		}
	}

	@Override
	public void drawCaves(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height) {
		ListTag sections = NbtHelper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = NbtHelper.tagFromCompound(root, "Level");

		height = MathUtil.clamp(height, 0, 255);

		ListTag[] palettes = new ListTag[16];
		long[][] blockStatesArray = new long[16][];
		sections.forEach(s -> {
			ListTag p = NbtHelper.tagFromCompound(s, "Palette");
			long[] b = NbtHelper.longArrayFromCompound(s, "BlockStates");
			int y = NbtHelper.numberFromCompound(s, "Y", -1).intValue();
			if (y >= 0 && y <= 15 && p != null && b != null) {
				palettes[y] = p;
				blockStatesArray[y] = b;
			}
		});

		int[] biomes = NbtHelper.intArrayFromCompound(level, "Biomes");

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				int ignored = 0;
				boolean doneSkipping = false;

				// loop over sections
				for (int i = palettes.length - (16 - (height >> 4)); i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						continue;
					}

					long[] blockStates = blockStatesArray[i];
					ListTag palette = palettes[i];

					int sectionHeight = i * Tile.CHUNK_SIZE;

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);

					int startHeight;
					if (height >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - height % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.getCompound(paletteIndex);

						if (!isEmptyOrFoliage(blockData, colorMapping)) {
							if (doneSkipping) {
								int regionIndex = (z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale);
								int biome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);
								biome = MathUtil.clamp(biome, 0, 255);
								pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
								terrainHeights[regionIndex] = (short) (sectionHeight + cy);
								continue zLoop;
							}
							ignored++;
						} else if (ignored > 0) {
							doneSkipping = true;
						}
					}
				}
			}
		}
	}

	@Override
	public CompoundTag minimizeChunk(CompoundTag root) {
		CompoundTag minData = new CompoundTag();
		minData.put("DataVersion", root.get("DataVersion").copy());
		CompoundTag level = new CompoundTag();
		minData.put("Level", level);
		level.put("Biomes", root.getCompound("Level").get("Biomes").copy());
		level.put("Sections", root.getCompound("Level").get("Sections").copy());
		level.put("Status", root.getCompound("Level").get("Status").copy());
		return minData;
	}

	private static final CompoundTag waterDummy = new CompoundTag();

	static {
		waterDummy.putString("Name", "minecraft:water");
	}

	private boolean isWater(CompoundTag blockData) {
		return switch (NbtHelper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:water", "minecraft:bubble_column" -> true;
			default -> false;
		};
	}

	private boolean isWaterlogged(CompoundTag data) {
		return data.get("Properties") != null && "true".equals(NbtHelper.stringFromCompound(NbtHelper.tagFromCompound(data, "Properties"), "waterlogged", null));
	}

	private boolean isEmpty(CompoundTag blockData) {
		return switch (NbtHelper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void" -> blockData.size() == 1;
			default -> false;
		};
	}

	private boolean isEmptyOrFoliage(CompoundTag blockData, ColorMapping colorMapping) {
		String name;
		return switch (name = NbtHelper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void", "minecraft:snow" -> blockData.size() == 1;
			default -> colorMapping.isFoliage(name);
		};
	}

	private int getIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}

	private int getBiomeIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE + z * 4 + x;
	}

	private int getBiomeAtBlock(int[] biomes, int biomeX, int biomeY, int biomeZ) {
		if (biomes == null || biomes.length != 1024) {
			return -1;
		}
		return biomes[getBiomeIndex(biomeX >> 2, biomeY >> 2, biomeZ >> 2)];
	}

	private int getPaletteIndex(int index, long[] blockStates, int bits, int clean) {
		double blockStatesIndex = index / (4096D / blockStates.length);

		int longIndex = (int) blockStatesIndex;
		int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);

		if (startBit + bits > 64) {
			// get msb from current long, no need to cleanup manually, just fill with 0
			int previous = (int) (blockStates[longIndex] >>> startBit);

			// cleanup pattern for bits from next long
			int remainingClean = ((int) Math.pow(2, startBit + bits - 64) - 1);

			// get lsb from next long
			int next = ((int) blockStates[longIndex + 1]) & remainingClean;
			return (next << 64 - startBit) + previous;
		} else {
			return (int) (blockStates[longIndex] >> startBit) & clean;
		}
	}
}
