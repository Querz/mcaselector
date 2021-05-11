package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil112ChunkRenderer implements ChunkRenderer {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water) {
		ListTag<CompoundTag> sections = withDefault(() -> root.getCompoundTag("Level").getListTag("Sections").asCompoundTagList(), null);
		if (sections == null) {
			return;
		}

		byte[][] blocksArray = new byte[16][];
		byte[][] dataArray = new byte[16][];
		sections.forEach(s -> {
			if (!s.containsKey("Blocks") || !s.containsKey("Data")) {
				return;
			}
			int y = withDefault(() -> s.getNumber("Y").intValue(), -1);
			byte[] b = withDefault(() -> s.getByteArray("Blocks"), null);
			byte[] d = withDefault(() -> s.getByteArray("Data"), null);
			if (y >= 0 && y < 16 && b != null && d != null) {
				blocksArray[y] = b;
				dataArray[y] = d;
			}
		});

		byte[] biomes = withDefault(() -> root.getCompoundTag("Level").getByteArray("Biomes"), null);

		//loop over x / z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {

				int biome = -1;
				if (biomes != null && biomes.length != 0) {
					biome = biomes[getBlockIndex(cx, 0, cz)] & 0xFF;
				}

				biome = Math.max(0, biome);

				boolean waterDepth = false;
				//loop over sections
				for (int i = blocksArray.length - 1; i >= 0; i--) {
					if (blocksArray[i] == null) {
						continue;
					}

					byte[] blocks = blocksArray[i];
					byte[] data = dataArray[i];

					int sectionHeight = i * Tile.CHUNK_SIZE;

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
									pixelBuffer[regionIndex] = colorMapping.getRGB(((block << 4) + blockData), biome);
									waterHeights[regionIndex] = (short) (sectionHeight + cy);
								}
								if (isWater(block)) {
									waterDepth = true;
									continue;
								} else {
									waterPixels[regionIndex] = colorMapping.getRGB(((block << 4) + blockData), biome);
								}
							} else {
								pixelBuffer[regionIndex] = colorMapping.getRGB(((block << 4) + blockData), biome);
							}
							terrainHeights[regionIndex] = (short) (sectionHeight + cy);
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
}
