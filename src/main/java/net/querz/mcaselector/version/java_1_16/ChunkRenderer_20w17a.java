package net.querz.mcaselector.version.java_1_16;

import net.querz.mcaselector.util.math.Bits;
import net.querz.mcaselector.util.math.MathUtil;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

@MCVersionImplementation(2529)
public class ChunkRenderer_20w17a implements ChunkRenderer<CompoundTag, Integer> {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping<CompoundTag, Integer> colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height) {
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
		ListTag[] indexedPalettes = new ListTag[yMax];
		LongBuffer[] indexedBlockStates = new LongBuffer[yMax];
		IntBuffer biomes = null;
		byte[] biomesData = Helper.byteArrayFromCompound(level, "Biomes");
		if (biomesData != null) {
			biomes = ByteBuffer.wrap(biomesData).asIntBuffer();
		}
		int[] bits = new int[yMax];
		int[] cleanBits = new int[yMax];
		int[] indexesPerLong = new int[yMax];
		int[] startHeight = new int[yMax];
		int[] sectionHeight = new int[yMax];
		int biome;
		int pixelIndex;

		for (int cx = 0; cx < 16; cx += scale) {
			zLoop:
			for (int cz = 0; cz < 16; cz += scale) {
				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				boolean waterDepth = false;
				for (int i = height >> 4; i >= 0; i--) {
					// no section --> nothing to do
					if (indexedSections[i] == null) {
						continue;
					}

					// if the section has not been indexed yet, index it now
					if (!indexed[i]) {
						CompoundTag section = indexedSections[i];
						indexedPalettes[i] = Helper.tagFromCompound(section, "Palette");
						byte[] data = Helper.byteArrayFromCompound(section, "BlockStates");
						if (data != null) {
							indexedBlockStates[i] = ByteBuffer.wrap(data).asLongBuffer();
						}
						// calculate how many bits each index uses based on the size of the blockStates array
						bits[i] = data == null ? 0 : data.length >> 9;
						// create a bitmask for the msb after the index has been shifted to the right
						cleanBits[i] = ((2 << (bits[i] - 1)) - 1);
						// calculate the amount of indexes per long
						indexesPerLong[i] = (int) (64D / bits[i]);

						// if we are in the section containing the highest y we need to render,
						// we calculate the highest y in the section with absHeight % 16
						// otherwise we start all the way at the top (15)
						startHeight[i] = height >> 4 == i ? height & 0xF : 15;
						// calculate height of section in blocks
						sectionHeight[i] = i * 16;

						indexed[i] = true;
					}

					ListTag palette = indexedPalettes[i];
					if (palette == null) {
						continue;
					}
					LongBuffer blockStates = indexedBlockStates[i];

					for (int cy = startHeight[i]; cy >= 0; cy--) {
						CompoundTag blockData = getBlock(cx, cy, cz, blockStates, bits[i], cleanBits[i], indexesPerLong[i], palette);
						if (colorMapping.isTransparent(blockData)) {
							continue;
						}

						biome = getBiome(cx, cy + sectionHeight[i], cz, biomes);

						if (water) {
							if (!waterDepth) {
								pixelBuffer[pixelIndex] = colorMapping.getRGB(blockData, biome); // water color
								waterHeights[pixelIndex] = (short) (sectionHeight[i] + cy); // height of highest water or terrain block
							}
							if (colorMapping.isWater(blockData)) {
								waterDepth = true;
								continue;
							} else if (colorMapping.isWaterlogged(blockData)) {
								pixelBuffer[pixelIndex] = colorMapping.getRGB(waterDummy, biome); // water color
								waterPixels[pixelIndex] = colorMapping.getRGB(blockData, biome); // color of waterlogged block
								waterHeights[pixelIndex] = (short) (sectionHeight[i] + cy);
								terrainHeights[pixelIndex] = (short) (sectionHeight[i] + cy - 1); // "height" of bottom of water, which will just be 1 block lower for shading
								continue zLoop;
							} else {
								waterPixels[pixelIndex] = colorMapping.getRGB(blockData, biome); // color of block at bottom of water
							}
						} else {
							pixelBuffer[pixelIndex] = colorMapping.getRGB(blockData, biome);
						}
						terrainHeights[pixelIndex] = (short) (sectionHeight[i] + cy);
						continue zLoop;
					}
				}
			}
		}
	}

	@Override
	public void drawLayer(CompoundTag root, ColorMapping<CompoundTag, Integer> colorMapping, int x, int z, int scale, int[] pixelBuffer, int height) {
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

		ListTag palette = Helper.tagFromCompound(section, "Palette");
		byte[] data = Helper.byteArrayFromCompound(section, "BlockStates");
		LongBuffer blockStates = null;
		if (data != null) {
			blockStates = ByteBuffer.wrap(data).asLongBuffer();
		}
		if (palette == null) {
			return;
		}

		IntBuffer biomes = null;
		byte[] biomesData = Helper.byteArrayFromCompound(level, "Biomes");
		if (biomesData != null) {
			biomes = ByteBuffer.wrap(biomesData).asIntBuffer();
		}

		int scaleBits = Bits.msbPosition(scale);
		int cy = height & 0xF;
		int bits = data == null ? 0 : data.length >> 9;
		int clean = ((2 << (bits - 1)) - 1);
		int indexesPerLong = (int) (64D / bits);
		int biome;
		int pixelIndex;

		for (int cx = 0; cx < 16; cx += scale) {
			for (int cz = 0; cz < 16; cz += scale) {
				CompoundTag blockData = getBlock(cx, cy, cz, blockStates, bits, clean, indexesPerLong, palette);
				if (colorMapping.isTransparent(blockData)) {
					continue;
				}
				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
				biome = getBiome(cx, height, cz, biomes);
				pixelBuffer[pixelIndex] = colorMapping.getRGB(blockData, biome);
			}
		}
	}

	@Override
	public void drawCaves(CompoundTag root, ColorMapping<CompoundTag, Integer> colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height) {
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
		ListTag[] indexedPalettes = new ListTag[yMax];
		LongBuffer[] indexedBlockStates = new LongBuffer[yMax];
		IntBuffer biomes = null;
		byte[] biomesData = Helper.byteArrayFromCompound(level, "Biomes");
		if (biomesData != null) {
			biomes = ByteBuffer.wrap(biomesData).asIntBuffer();
		}
		int[] bits = new int[yMax];
		int[] cleanBits = new int[yMax];
		int[] indexesPerLong = new int[yMax];
		int[] startHeight = new int[yMax];
		int[] sectionHeight = new int[yMax];
		int biome;
		int pixelIndex;

		for (int cx = 0; cx < 16; cx += scale) {
			zLoop:
			for (int cz = 0; cz < 16; cz += scale) {
				pixelIndex = (z + (cz >> scaleBits)) * (512 >> scaleBits) + (x + (cx >> scaleBits));
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
						indexedPalettes[i] = Helper.tagFromCompound(section, "Palette");
						byte[] data = Helper.byteArrayFromCompound(section, "BlockStates");
						if (data != null) {
							indexedBlockStates[i] = ByteBuffer.wrap(data).asLongBuffer();
						}
						// calculate how many bits each index uses based on the size of the blockStates array
						bits[i] = data == null ? 0 : data.length >> 9;
						// create a bitmask for the msb after the index has been shifted to the right
						cleanBits[i] = ((2 << (bits[i] - 1)) - 1);
						// calculate the amount of indexes per long
						indexesPerLong[i] = (int) (64D / bits[i]);

						// if we are in the section containing the highest y we need to render,
						// we calculate the highest y in the section with absHeight % 16
						// otherwise we start all the way at the top (15)
						startHeight[i] = height >> 4 == i ? height & 0xF : 15;
						// calculate height of section in blocks
						sectionHeight[i] = i * 16;

						indexed[i] = true;
					}

					ListTag palette = indexedPalettes[i];
					if (palette == null) {
						continue;
					}
					LongBuffer blockStates = indexedBlockStates[i];

					for (int cy = startHeight[i]; cy >= 0; cy--) {
						CompoundTag blockData = getBlock(cx, cy, cz, blockStates, bits[i], cleanBits[i], indexesPerLong[i], palette);

						if (!colorMapping.isTransparent(blockData) && !colorMapping.isFoliage(blockData)) {
							if (doneSkipping) {
								biome = getBiome(cx, cy + sectionHeight[i], cz, biomes);

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

	private CompoundTag getBlock(int x, int y, int z, LongBuffer blockStates, int bits, int clean, int indexesPerLong, ListTag palette) {
		if (bits == 0) {
			return palette.getCompound(0);
		}
		int index = y * 256 + z * 16 + x;
		int blockStatesIndex = index / indexesPerLong;
		int startBit = (index % indexesPerLong) * bits;
		return palette.getCompound((int) (blockStates.get(blockStatesIndex) >> startBit) & clean);
	}

	private int getBiome(int x, int y, int z, IntBuffer biomes) {
		if (biomes == null || biomes.limit() != 1024) {
			return 0;
		}
		int b = biomes.get((y >> 2 << 4) + (z >> 2 << 2) + (x >> 2));
		return b < 0 || b > 255 ? 0 : b;
	}
}
