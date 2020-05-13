package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.version.ChunkDataProcessor;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.tiles.Tile;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil112ChunkDataProcessor implements ChunkDataProcessor {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, short[] heights, boolean water) {
		ListTag<CompoundTag> sections = withDefault(() -> root.getCompoundTag("Level").getListTag("Sections").asCompoundTagList(), null);
		if (sections == null) {
			return;
		}
		sections.sort(this::filterSections);

		//loop over x / z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {

				byte[] biomes = withDefault(() -> root.getCompoundTag("Level").getByteArray("Biomes"), null);
				int biome = -1;
				if (biomes != null && biomes.length != 0) {
					biome = biomes[getBlockIndex(cx, 0, cz)];
				}

				boolean waterDepth = false;
				//loop over sections
				sLoop: for (int i = 0; i < sections.size(); i++) {
					final int si = i;
					byte[] blocks = withDefault(() -> sections.get(si).getByteArray("Blocks"), null);
					if (blocks == null) {
						continue;
					}
					byte[] data = withDefault(() -> sections.get(si).getByteArray("Data"), null);
					if (data == null) {
						continue;
					}

					Byte height = withDefault(() -> sections.get(si).getByte("Y"), null);
					if (height == null) {
						continue;
					}
					int sectionHeight = height * 16;

					//loop over y value in section from top to bottom
					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						int index = getBlockIndex(cx, cy, cz);
						short block = (short) (blocks[index] & 0xFF);

						//ignore bedrock and netherrack until 75
						if (isIgnoredInNether(biome, block, sectionHeight + cy)) {
							continue;
						}

						byte blockData = (byte) (index % 2 == 0 ? data[index / 2] & 0x0F : (data[index / 2] >> 4) & 0x0F);

						if (!isEmpty(block)) {
							int regionIndex = (z + cz) * Tile.SIZE + (x + cx);
							if (water) {
								if (!waterDepth) {
									pixelBuffer[regionIndex] = colorMapping.getRGB(((block << 4) + blockData)) | 0xFF000000;
								}
								if (isWater(block)) {
									waterDepth = true;
									continue sLoop;
								}
							} else {
								pixelBuffer[regionIndex] = colorMapping.getRGB(((block << 4) + blockData)) | 0xFF000000;
							}
							heights[regionIndex] = (short) (sectionHeight + cy);
							continue zLoop;
						}
					}
				}
			}
		}
	}

	private boolean isWater(short block) {
		switch (block) {
		case 8:
		case 9:
			return true;
		}
		return false;
	}

	private boolean isIgnoredInNether(int biome, short block, int height) {
		if (biome == 8) {
			switch (block) {
			case 7:   //bedrock
			case 10:  //flowing_lava
			case 11:  //lava
			case 87:  //netherrack
			case 153: //quartz_ore
				return height > 75;
			}
		}
		return false;
	}

	private boolean isEmpty(int blockID) {
		return blockID == 0 || blockID == 166 || blockID == 217;
	}

	private int getBlockIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}

	private int filterSections(CompoundTag sectionA, CompoundTag sectionB) {
		return withDefault(() -> sectionB.getByte("Y"), (byte) -1) - withDefault(() -> sectionA.getByte("Y"), (byte) -1);
	}
}
