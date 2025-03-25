package net.querz.mcaselector.version.java_1_9;

import net.querz.mcaselector.util.math.Bits;
import net.querz.mcaselector.util.math.MathUtil;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

@MCVersionImplementation(100)
public class ChunkRenderer_15w32a implements ChunkRenderer<Integer, Integer> {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping<Integer, Integer> colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height) {
		ListTag sections = Helper.tagFromLevelFromRoot(root, "Sections");
		if (sections == null || sections.getElementType() != Tag.Type.COMPOUND) {
			return;
		}

		CompoundTag level = Helper.tagFromCompound(root, "Level");

		height = MathUtil.clamp(height, 0, 255);
		int scaleBits = Bits.msbPosition(scale);
		int yMax = 1 + (height >> 4);

		CompoundTag[] indexedSections = new CompoundTag[yMax];
		sections.iterateType(CompoundTag.class).forEach(s -> {
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= 0 && y < yMax) {
				indexedSections[y] = s;
			}
		});
		boolean[] indexed = new boolean[yMax];
		byte[][] indexedBlocks = new byte[yMax][];
		byte[][] indexedData = new byte[yMax][];
		byte[] biomes = Helper.byteArrayFromCompound(level, "Biomes");
		int[] startHeight = new int[yMax];
		int[] sectionHeight = new int[yMax];
		byte[] blocks, blockData;
		int index, block, data, biome;
		int pixelIndex;

		for (int cx = 0; cx < 16; cx += scale) {
			zLoop:
			for (int cz = 0; cz < 16; cz += scale) {
				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				biome = getBiome(cx, cz, biomes);
				boolean waterDepth = false;
				for (int i = height >> 4; i >= 0; i--) {
					// no section --> nothing to do
					if (indexedSections[i] == null) {
						continue;
					}

					// if the section has not been indexed yet, index it now
					if (!indexed[i]) {
						CompoundTag section = indexedSections[i];
						if (section.containsKey("Blocks") && section.containsKey("Data")) {
							indexedBlocks[i] = Helper.byteArrayFromCompound(section, "Blocks");
							indexedData[i] = Helper.byteArrayFromCompound(section, "Data");
						}

						// if we are in the section containing the highest y we need to render,
						// we calculate the highest y in the section with absHeight % 16
						// otherwise we start all the way at the top (15)
						startHeight[i] = height >> 4 == i ? height & 0xF : 15;
						// calculate height of section in blocks
						sectionHeight[i] = i * 16;

						indexed[i] = true;
					}

					blocks = indexedBlocks[i];
					if (blocks == null) {
						continue;
					}
					blockData = indexedData[i];

					for (int cy = startHeight[i]; cy >= 0; cy--) {
						index = cy * 256 + cz * 16 + cx;
						block = blocks[index] & 0xFF;
						data = ((index & 0x1) == 0 ? blockData[index >> 1] : blockData[index >> 1] >> 4) & 0x0F;

						if (colorMapping.isTransparent(block)) {
							continue;
						}

						if (water) {
							if (!waterDepth) {
								pixelBuffer[pixelIndex] = colorMapping.getRGB((block << 4) + data, biome); // water color
								waterHeights[pixelIndex] = (short) (sectionHeight[i] + cy); // height of highest water or terrain block
							}
							if (colorMapping.isWater(block)) {
								waterDepth = true;
								continue;
							} else {
								waterPixels[pixelIndex] = colorMapping.getRGB((block << 4) + data, biome);
							}
						} else {
							pixelBuffer[pixelIndex] = colorMapping.getRGB((block << 4) + data, biome);
						}
						terrainHeights[pixelIndex] = (short) (sectionHeight[i] + cy);
						continue zLoop;
					}
				}
			}
		}
	}

	public void drawLayer(CompoundTag root, ColorMapping<Integer, Integer> colorMapping, int x, int z, int scale, int[] pixelBuffer, int height) {
		ListTag sections = Helper.tagFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = Helper.tagFromCompound(root, "Level");
		height = MathUtil.clamp(height, 0, 255);

		CompoundTag section = null;
		for (CompoundTag s : sections.iterateType(CompoundTag.class)) {
			int y = Helper.numberFromCompound(s, "Y", -1).intValue();
			if (y == height >> 4) {
				section = s;
				break;
			}
		}
		if (section == null) {
			return;
		}

		byte[] blocks = Helper.byteArrayFromCompound(section, "Blocks");
		byte[] blockData = Helper.byteArrayFromCompound(section, "Data");
		if (blocks == null || blockData == null) {
			return;
		}

		byte[] biomes = Helper.byteArrayFromCompound(level, "Biomes");
		int scaleBits = Bits.msbPosition(scale);
		int cy = height & 0xF;
		int index, block, data, biome;
		int pixelIndex;

		for (int cx = 0; cx < 16; cx += scale) {
			for (int cz = 0; cz < 16; cz += scale) {
				index = cy * 256 + cz * 16 + cx;
				block = blocks[index] & 0xFF;
				if (colorMapping.isTransparent(block)) {
					continue;
				}
				data = ((index & 0x1) == 0 ? blockData[index >> 1] : blockData[index >> 1] >> 4) & 0x0F;

				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				biome = getBiome(cx, cz, biomes);
				pixelBuffer[pixelIndex] = colorMapping.getRGB((block << 4) + data, biome);
			}
		}
	}

	public void drawCaves(CompoundTag root, ColorMapping<Integer, Integer> colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height) {
		ListTag sections = Helper.tagFromLevelFromRoot(root, "Sections");
		if (sections == null || sections.getElementType() != Tag.Type.COMPOUND) {
			return;
		}

		CompoundTag level = Helper.tagFromCompound(root, "Level");

		height = MathUtil.clamp(height, 0, 255);
		int scaleBits = Bits.msbPosition(scale);
		int yMax = 1 + (height >> 4);

		CompoundTag[] indexedSections = new CompoundTag[yMax];
		sections.iterateType(CompoundTag.class).forEach(s -> {
			int y = Helper.numberFromCompound(s, "Y", -1).intValue();
			if (y >= 0 && y < yMax) {
				indexedSections[y] = s;
			}
		});
		boolean[] indexed = new boolean[yMax];
		byte[][] indexedBlocks = new byte[yMax][];
		byte[][] indexedData = new byte[yMax][];
		byte[] biomes = Helper.byteArrayFromCompound(level, "Biomes");
		int[] startHeight = new int[yMax];
		int[] sectionHeight = new int[yMax];
		byte[] blocks, blockData;
		int index, block, data, biome;
		int pixelIndex;

		for (int cx = 0; cx < 16; cx += scale) {
			zLoop:
			for (int cz = 0; cz < 16; cz += scale) {
				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				biome = getBiome(cx, cz, biomes);
				int ignored = 0;
				boolean doneSkipping = false;
				for (int i = height >> 4; i >= 0; i--) {
					// no section --> nothing to do
					if (indexedSections[i] == null) {
						continue;
					}

					// if the section has not been indexed yet, index it now
					if (!indexed[i]) {
						CompoundTag section = indexedSections[i];
						if (section.containsKey("Blocks") && section.containsKey("Data")) {
							indexedBlocks[i] = Helper.byteArrayFromCompound(section, "Blocks");
							indexedData[i] = Helper.byteArrayFromCompound(section, "Data");
						}

						// if we are in the section containing the highest y we need to render,
						// we calculate the highest y in the section with absHeight % 16
						// otherwise we start all the way at the top (15)
						startHeight[i] = height >> 4 == i ? height & 0xF : 15;
						// calculate height of section in blocks
						sectionHeight[i] = i * 16;

						indexed[i] = true;
					}

					blocks = indexedBlocks[i];
					if (blocks == null) {
						continue;
					}
					blockData = indexedData[i];

					for (int cy = startHeight[i]; cy >= 0; cy--) {
						index = cy * 256 + cz * 16 + cx;
						block = blocks[index] & 0xFF;
						data = ((index & 0x1) == 0 ? blockData[index >> 1] : blockData[index >> 1] >> 4) & 0x0F;
						if (!colorMapping.isTransparent(block) && !colorMapping.isFoliage(block)) {
							if (doneSkipping) {
								pixelBuffer[pixelIndex] = colorMapping.getRGB((block << 4) + data, biome);
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
		CompoundTag minData = new CompoundTag();
		minData.put("DataVersion", root.get("DataVersion").copy());
		CompoundTag level = new CompoundTag();
		minData.put("Level", level);
		level.put("Biomes", root.getCompound("Level").get("Biomes").copy());
		level.put("Sections", root.getCompound("Level").get("Sections").copy());
		level.put("Status", root.getCompound("Level").get("Status").copy());
		return minData;
	}

	private int getBiome(int x, int z, byte[] biomes) {
		if (biomes == null || biomes.length != 256) {
			return 0;
		}
		return biomes[z * 16 + x] & 0xFF;
	}
}
