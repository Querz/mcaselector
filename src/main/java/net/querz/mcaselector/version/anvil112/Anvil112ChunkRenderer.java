package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.math.MathUtil;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;

public class Anvil112ChunkRenderer implements ChunkRenderer {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height) {
		ListTag sections = NbtHelper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		byte[][] blocksArray = new byte[16][];
		byte[][] dataArray = new byte[16][];
		for (CompoundTag s : sections.iterateType(CompoundTag.TYPE)) {
			if (!s.containsKey("Blocks") || !s.containsKey("Data")) {
				return;
			}
			int y = NbtHelper.numberFromCompound(s, "Y", -1).intValue();
			byte[] b = NbtHelper.byteArrayFromCompound(s, "Blocks");
			byte[] d = NbtHelper.byteArrayFromCompound(s, "Data");
			if (y >= 0 && y < 16 && b != null && d != null) {
				blocksArray[y] = b;
				dataArray[y] = d;
			}
		}

		height = MathUtil.clamp(height, 0, 255);

		byte[] biomes = NbtHelper.byteArrayFromCompound(NbtHelper.tagFromCompound(root, "Level"), "Biomes");

		// loop over x / z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				int biome = -1;
				if (biomes != null && biomes.length != 0) {
					biome = biomes[getBlockIndex(cx, 0, cz)] & 0xFF;
				}
				biome = Math.max(0, biome);

				boolean waterDepth = false;
				// loop over sections
				for (int i = blocksArray.length - (16 - (height >> 4)); i >= 0; i--) {
					if (blocksArray[i] == null) {
						continue;
					}

					byte[] blocks = blocksArray[i];
					byte[] data = dataArray[i];

					int sectionHeight = i * Tile.CHUNK_SIZE;

					int startHeight;
					if (height >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - height % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					// loop over y value in section from top to bottom
					for (int cy = startHeight; cy >= 0; cy--) {
						int index = getBlockIndex(cx, cy, cz);
						short block = (short) (blocks[index] & 0xFF);

						byte blockData = (byte) (index % 2 == 0 ? data[index / 2] & 0x0F : (data[index / 2] >> 4) & 0x0F);

						if (isEmpty(block)) {
							continue;
						}

						int regionIndex = (z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale);
						if (water) {
							if (!waterDepth) {
								pixelBuffer[regionIndex] = colorMapping.getRGB((block << 4) + blockData, biome);
								waterHeights[regionIndex] = (short) (sectionHeight + cy);
							}
							if (isWater(block)) {
								waterDepth = true;
								continue;
							} else {
								waterPixels[regionIndex] = colorMapping.getRGB((block << 4) + blockData, biome);
							}
						} else {
							pixelBuffer[regionIndex] = colorMapping.getRGB((block << 4) + blockData, biome);
						}
						terrainHeights[regionIndex] = (short) (sectionHeight + cy);
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

		DataProperty<CompoundTag> section = new DataProperty<>();
		for (CompoundTag s : sections.iterateType(CompoundTag.TYPE)) {
			int y = NbtHelper.numberFromCompound(s, "Y", -1).intValue();
			if (y == height >> 4) {
				section.set(s);
				break;
			}
		}
		if (section.get() == null) {
			return;
		}

		byte[] blocks = NbtHelper.byteArrayFromCompound(section.get(), "Blocks");
		byte[] data = NbtHelper.byteArrayFromCompound(section.get(), "Data");
		if (blocks == null || data == null) {
			return;
		}

		byte[] biomes = NbtHelper.byteArrayFromCompound(NbtHelper.tagFromCompound(root, "Level"), "Biomes");

		height = MathUtil.clamp(height, 0, 255);

		int cy = height % 16;

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {
				int index = getBlockIndex(cx, cy, cz);
				short block = (short) (blocks[index] & 0xFF);

				if (isEmpty(block)) {
					continue;
				}

				byte blockData = (byte) (index % 2 == 0 ? data[index / 2] & 0x0F : (data[index / 2] >> 4) & 0x0F);

				int biome = -1;
				if (biomes != null && biomes.length != 0) {
					biome = biomes[getBlockIndex(cx, 0, cz)] & 0xFF;
				}
				biome = Math.max(0, biome);

				int regionIndex = (z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale);
				pixelBuffer[regionIndex] = colorMapping.getRGB((block << 4) + blockData, biome);
			}
		}
	}

	@Override
	public void drawCaves(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height) {
		ListTag sections = NbtHelper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		byte[][] blocksArray = new byte[16][];
		byte[][] dataArray = new byte[16][];
		for (CompoundTag s : sections.iterateType(CompoundTag.TYPE)) {
			if (!s.containsKey("Blocks") || !s.containsKey("Data")) {
				return;
			}
			int y = NbtHelper.numberFromCompound(s, "Y", -1).intValue();
			byte[] b = NbtHelper.byteArrayFromCompound(s, "Blocks");
			byte[] d = NbtHelper.byteArrayFromCompound(s, "Data");
			if (y >= 0 && y < 16 && b != null && d != null) {
				blocksArray[y] = b;
				dataArray[y] = d;
			}
		}

		height = MathUtil.clamp(height, 0, 255);

		byte[] biomes = NbtHelper.byteArrayFromCompound(NbtHelper.tagFromCompound(root, "Level"), "Biomes");

		// loop over x / z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				int ignored = 0;
				boolean doneSkipping = false;

				// loop over sections
				for (int i = blocksArray.length - (16 - (height >> 4)); i >= 0; i--) {
					if (blocksArray[i] == null) {
						continue;
					}

					byte[] blocks = blocksArray[i];
					byte[] data = dataArray[i];

					int sectionHeight = i * Tile.CHUNK_SIZE;

					int startHeight;
					if (height >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - height % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					// loop over y value in section from top to bottom
					for (int cy = startHeight; cy >= 0; cy--) {
						int index = getBlockIndex(cx, cy, cz);
						short block = (short) (blocks[index] & 0xFF);

						byte blockData = (byte) (index % 2 == 0 ? data[index / 2] & 0x0F : (data[index / 2] >> 4) & 0x0F);

						if (!isEmptyOrFoliage(block, colorMapping)) {
							if (doneSkipping) {
								int regionIndex = (z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale);
								int biome = -1;
								if (biomes != null && biomes.length != 0) {
									biome = biomes[getBlockIndex(cx, 0, cz)] & 0xFF;
								}
								biome = Math.max(0, biome);
								pixelBuffer[regionIndex] = colorMapping.getRGB((block << 4) + blockData, biome);
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

	private boolean isWater(short block) {
		return switch (block) {
			case 8, 9 -> true;
			default -> false;
		};
	}

	private boolean isEmpty(int blockID) {
		return blockID == 0 || blockID == 166 || blockID == 217;
	}

	private boolean isEmptyOrFoliage(int blockID, ColorMapping colorMapping) {
		return switch (blockID) {
			case 0, 166, 217, 78 -> true;
			default -> colorMapping.isFoliage(blockID);
		};
	}

	private int getBlockIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}
}
