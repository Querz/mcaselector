package net.querz.mcaselector.version.java_1_18;

import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

@MCVersionImplementation(2834)
public class ChunkRenderer_21w37a implements ChunkRenderer {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height) {
		ListTag sections = Helper.tagFromCompound(root, "sections");
		if (sections == null || sections.getElementType() != Tag.Type.COMPOUND) {
			return;
		}

		int scaleBits = Bits.msbPosition(scale);
		int absHeight = height + 64;
		int yMax = 1 + (height >> 4);
		int sMax = yMax + 4;

		CompoundTag[] indexedSections = new CompoundTag[sMax];
		sections.iterateType(CompoundTag.class).forEach(s -> {
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y < yMax) {
				indexedSections[y + 4] = s;
			}
		});
		boolean[] indexed = new boolean[sMax];
		ListTag[] indexedPalettes = new ListTag[sMax];
		LongBuffer[] indexedBlockStates = new LongBuffer[sMax];
		ListTag[] indexedBiomePalettes = new ListTag[sMax];
		LongBuffer[] indexedBiomeData = new LongBuffer[sMax];
		int[] bits = new int[sMax];
		int[] cleanBits = new int[sMax];
		int[] indexesPerLong = new int[sMax];
		int[] biomeBits = new int[sMax];
		int[] biomeCleanBits = new int[sMax];
		int[] biomeIndexesPerLong = new int[sMax];
		int[] startHeight = new int[sMax];
		int[] sectionHeight = new int[sMax];
		int paletteIndex;
		int biomeIndex;
		int pixelIndex;
		String biome;

		for (int cx = 0; cx < 16; cx += scale) {
			zLoop:
			for (int cz = 0; cz < 16; cz += scale) {
				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				boolean waterDepth = false;
				for (int i = sMax - (sMax - (absHeight >> 4)); i >= 0; i--) {
					// no section --> nothing to do
					if (indexedSections[i] == null) {
						continue;
					}

					// if the section has not been indexed yet, index it now
					if (!indexed[i]) {
						CompoundTag section = indexedSections[i];
						indexedPalettes[i] = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
						byte[] data = Helper.byteArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
						if (data != null) {
							indexedBlockStates[i] = ByteBuffer.wrap(data).asLongBuffer();
						}
						indexedBiomePalettes[i] = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
						byte[] biomes = Helper.byteArrayFromCompound(Helper.tagFromCompound(section, "biomes"), "data");
						if (biomes != null) {
							indexedBiomeData[i] = ByteBuffer.wrap(biomes).asLongBuffer();
						}
						// calculate how many bits each index uses based on the size of the blockStates array
						bits[i] = data == null ? 0 : data.length >> 9;
						// create a bitmask for the msb after the index has been shifted to the right
						cleanBits[i] = ((2 << (bits[i] - 1)) - 1);
						// calculate the amount of indexes per long
						indexesPerLong[i] = (int) (64D / bits[i]);

						biomeBits[i] = biomes == null ? 0 : biomes.length >> 3;
						biomeCleanBits[i] = ((2 << (biomeBits[i] - 1)) - 1);
						biomeIndexesPerLong[i] = (int) (64D / biomeBits[i]);

						// if we are in the section containing the highest y we need to render,
						// we calculate the highest y in the section with absHeight % 16
						// otherwise we start all the way at the top (15)
						startHeight[i] = absHeight >> 4 == i ? absHeight & 0xF : 15;
						// calculate height of section in blocks
						sectionHeight[i] = (i - 4) * 16;

						indexed[i] = true;
					}

					ListTag palette = indexedPalettes[i];
					if (palette == null) {
						continue;
					}
					LongBuffer blockStates = indexedBlockStates[i];
					LongBuffer biomes = indexedBiomeData[i];
					ListTag biomePalette = indexedBiomePalettes[i];

					for (int cy = startHeight[i]; cy >= 0; cy--) {
						paletteIndex = getPaletteIndex(cx, cy, cz, blockStates, bits[i], cleanBits[i], indexesPerLong[i]);
						CompoundTag blockData = palette.getCompound(paletteIndex);
						if (isEmpty(blockData)) {
							continue;
						}

						biomeIndex = getBiomeIndex(cx, cy, cz, biomes, biomeBits[i], biomeCleanBits[i], biomeIndexesPerLong[i]);
						biome = getBiome(biomeIndex, biomePalette);

						if (water) {
							if (!waterDepth) {
								pixelBuffer[pixelIndex] = colorMapping.getRGB(blockData, biome); // water color
								waterHeights[pixelIndex] = (short) (sectionHeight[i] + cy); // height of highest water or terrain block
							}
							if (isWater(blockData)) {
								waterDepth = true;
								continue;
							} else if (isWaterlogged(blockData)) {
								pixelBuffer[pixelIndex] = colorMapping.getRGB(waterDummy, biome); // water color
								waterPixels[pixelIndex] = colorMapping.getRGB(blockData, biome); // color of waterlogged block
								waterHeights[pixelIndex] = (short) (sectionHeight[i] + cy);
								terrainHeights[pixelIndex] = (short) (sectionHeight[i] + cy - 1); // "height" of bottom of water, which will just be 1 block lower for shading
								continue zLoop;
							} else {
								waterPixels[pixelIndex] = colorMapping.getRGB(blockData, biome); // color of block at bottom of water
							}
						} else {
							waterPixels[pixelIndex] = colorMapping.getRGB(blockData, biome);
						}
						terrainHeights[pixelIndex] = (short) (sectionHeight[i] + cy);
						continue zLoop;
					}
				}
			}
		}
	}

	@Override
	public void drawLayer(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int height) {
		ListTag sections = Helper.tagFromCompound(root, "sections");
		if (sections == null) {
			return;
		}

		CompoundTag section = null;
		for (CompoundTag s : sections.iterateType(CompoundTag.class)) {
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y == height >> 4) {
				section = s;
				break;
			}
		}
		if (section == null) {
			return;
		}

		ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
		byte[] data = Helper.byteArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
		LongBuffer blockStates = null;
		if (data != null) {
			blockStates = ByteBuffer.wrap(data).asLongBuffer();
		}
		if (palette == null) {
			return;
		}

		ListTag biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
		byte[] biomeData = Helper.byteArrayFromCompound(Helper.tagFromCompound(section, "biomes"), "data");
		LongBuffer biomes = null;
		if (biomeData != null) {
			biomes = ByteBuffer.wrap(biomeData).asLongBuffer();
		}

		int scaleBits = Bits.msbPosition(scale);
		int absHeight = height + 64;
		int cy = absHeight & 0xF;
		int bits = data == null ? 0 : data.length >> 9;
		int clean = ((2 << (bits - 1)) - 1);
		int indexesPerLong = (int) (64D / bits);
		int biomeBits = biomeData == null ? 0 : biomeData.length >> 3;
		int biomeCleanBits = ((2 << (biomeBits - 1)) - 1);
		int biomeIndexesPerLong = (int) (64D / biomeBits);
		int biomeIndex;
		int pixelIndex;
		String biome;

		for (int cx = 0; cx < 16; cx += scale) {
			for (int cz = 0; cz < 16; cz += scale) {
				int paletteIndex = getPaletteIndex(cx, cy, cz, blockStates, bits, clean, indexesPerLong);
				CompoundTag blockData = palette.getCompound(paletteIndex);
				if (isEmpty(blockData)) {
					continue;
				}
				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				biomeIndex = getBiomeIndex(cx, cy, cz, biomes, biomeBits, biomeCleanBits, biomeIndexesPerLong);
				biome = getBiome(biomeIndex, biomePalette);
				pixelBuffer[pixelIndex] = colorMapping.getRGB(blockData, biome);
			}
		}
	}

	public void drawCaves(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height) {
		ListTag sections = Helper.tagFromCompound(root, "sections");
		if (sections == null || sections.getElementType() != Tag.Type.COMPOUND) {
			return;
		}

		int scaleBits = Bits.msbPosition(scale);
		int absHeight = height + 64;
		int yMax = 1 + (height >> 4);
		int sMax = yMax + 4;

		CompoundTag[] indexedSections = new CompoundTag[sMax];
		sections.iterateType(CompoundTag.class).forEach(s -> {
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y < yMax) {
				indexedSections[y + 4] = s;
			}
		});
		boolean[] indexed = new boolean[sMax];
		ListTag[] indexedPalettes = new ListTag[sMax];
		LongBuffer[] indexedBlockStates = new LongBuffer[sMax];
		ListTag[] indexedBiomePalettes = new ListTag[sMax];
		LongBuffer[] indexedBiomeData = new LongBuffer[sMax];
		int[] bits = new int[sMax];
		int[] cleanBits = new int[sMax];
		int[] indexesPerLong = new int[sMax];
		int[] biomeBits = new int[sMax];
		int[] biomeCleanBits = new int[sMax];
		int[] biomeIndexesPerLong = new int[sMax];
		int[] startHeight = new int[sMax];
		int[] sectionHeight = new int[sMax];
		int paletteIndex;
		int biomeIndex;
		int pixelIndex;
		String biome;

		for (int cx = 0; cx < 16; cx += scale) {
			zLoop:
			for (int cz = 0; cz < 16; cz += scale) {
				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				int ignored = 0;
				boolean doneSkipping = false;
				for (int i = sMax - (sMax - (absHeight >> 4)); i >= 0; i--) {
					// no section --> nothing to do
					if (indexedSections[i] == null) {
						continue;
					}

					// if the section has not been indexed yet, index it now
					if (!indexed[i]) {
						CompoundTag section = indexedSections[i];
						indexedPalettes[i] = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
						byte[] data = Helper.byteArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
						if (data != null) {
							indexedBlockStates[i] = ByteBuffer.wrap(data).asLongBuffer();
						}
						indexedBiomePalettes[i] = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
						byte[] biomes = Helper.byteArrayFromCompound(Helper.tagFromCompound(section, "biomes"), "data");
						if (biomes != null) {
							indexedBiomeData[i] = ByteBuffer.wrap(biomes).asLongBuffer();
						}
						// calculate how many bits each index uses based on the size of the blockStates array
						bits[i] = data == null ? 0 : data.length >> 9;
						// create a bitmask for the msb after the index has been shifted to the right
						cleanBits[i] = ((2 << (bits[i] - 1)) - 1);
						// calculate the amount of indexes per long
						indexesPerLong[i] = (int) (64D / bits[i]);

						biomeBits[i] = biomes == null ? 0 : biomes.length >> 3;
						biomeCleanBits[i] = ((2 << (biomeBits[i] - 1)) - 1);
						biomeIndexesPerLong[i] = (int) (64D / biomeBits[i]);

						// if we are in the section containing the highest y we need to render,
						// we calculate the highest y in the section with absHeight % 16
						// otherwise we start all the way at the top (15)
						startHeight[i] = absHeight >> 4 == i ? absHeight & 0xF : 15;
						// calculate height of section in blocks
						sectionHeight[i] = (i - 4) * 16;

						indexed[i] = true;
					}

					ListTag palette = indexedPalettes[i];
					if (palette == null) {
						continue;
					}
					LongBuffer blockStates = indexedBlockStates[i];
					LongBuffer biomes = indexedBiomeData[i];
					ListTag biomePalette = indexedBiomePalettes[i];

					for (int cy = startHeight[i]; cy >= 0; cy--) {
						paletteIndex = getPaletteIndex(cx, cy, cz, blockStates, bits[i], cleanBits[i], indexesPerLong[i]);
						CompoundTag blockData = palette.getCompound(paletteIndex);

						if (!isEmptyOrFoliage(blockData, colorMapping)) {
							if (doneSkipping) {
								biomeIndex = getBiomeIndex(cx, cy, cz, biomes, biomeBits[i], biomeCleanBits[i], biomeIndexesPerLong[i]);
								biome = getBiome(biomeIndex, biomePalette);

								pixelBuffer[pixelIndex] = colorMapping.getRGB(blockData, biome);
								terrainHeights[pixelIndex] = (short) (sectionHeight[i] + cy);
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
		Integer dataVersion = Helper.intFromCompound(root, "DataVersion");
		if (dataVersion == null) {
			return root;
		}
		CompoundTag minData = new CompoundTag();
		minData.put("DataVersion", root.get("DataVersion").copy());
		minData.put("sections", root.get("sections").copy());
		minData.put("Status", root.get("Status").copy());
		return minData;
	}

	private static final CompoundTag waterDummy = new CompoundTag();

	static {
		waterDummy.putString("Name", "minecraft:water");
	}

	private boolean isWater(CompoundTag blockData) {
		return switch (Helper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:water", "minecraft:bubble_column" -> true;
			default -> false;
		};
	}

	private boolean isWaterlogged(CompoundTag data) {
		return data.get("Properties") != null && "true".equals(Helper.stringFromCompound(Helper.tagFromCompound(data, "Properties"), "waterlogged", null));
	}

	private boolean isEmpty(CompoundTag blockData) {
		return switch (Helper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void", "minecraft:light" -> blockData.size() == 1;
			default -> false;
		};
	}

	private boolean isEmptyOrFoliage(CompoundTag blockData, ColorMapping colorMapping) {
		String name;
		return switch (name = Helper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void", "minecraft:light", "minecraft:snow" -> blockData.size() == 1;
			default -> colorMapping.isFoliage(name);
		};
	}

	private static int getBiomeIndex(int x, int y, int z, LongBuffer biomes, int bits, int clean, int indexesPerLong) {
		if (bits == 0) {
			return 0;
		}
		int index = (y >> 2 & 0xF) * 16 + (z >> 2 & 0xF) * 4 + (x >> 2 & 0xF);
		int biomeIndex = index / indexesPerLong;
		int startBit = (index % indexesPerLong) * bits;
		return (int) (biomes.get(biomeIndex) >> startBit) & clean;
	}

	private String getBiome(int index, ListTag palette) {
		if (palette == null || palette.isEmpty() || index >= palette.size()) {
			return "";
		}
		return palette.getString(index);
	}

	private int getPaletteIndex(int x, int y, int z, LongBuffer blockStates, int bits, int clean, int indexesPerLong) {
		if (bits == 0) {
			return 0;
		}
		int index = y * 256 + z * 16 + x;
		int blockStatesIndex = index / indexesPerLong;
		int startBit = (index % indexesPerLong) * bits;
		return (int) (blockStates.get(blockStatesIndex) >> startBit) & clean;
	}
}
