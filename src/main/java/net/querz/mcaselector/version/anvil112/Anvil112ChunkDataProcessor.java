package net.querz.mcaselector.version.anvil112;

import javafx.scene.image.PixelWriter;
import net.querz.mcaselector.version.ChunkDataProcessor;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.tiles.Tile;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

public class Anvil112ChunkDataProcessor implements ChunkDataProcessor {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, PixelWriter writer) {
		ListTag sections = (ListTag) ((CompoundTag) root.get("Level")).get("Sections");
		sections.getValue().sort(this::filterSections);

		//loop over x / z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {

				byte[] biomes = ((CompoundTag) root.get("Level")).getBytes("Biomes");
				int biome = -1;
				if (biomes.length != 0) {
					biome = biomes[getBlockIndex(cx, 0, cz)];
				}

				//loop over sections
				for (int i = 0; i < sections.size(); i++) {

					byte[] blocks = ((CompoundTag) sections.get(i)).getBytes("Blocks");
					byte[] data = ((CompoundTag) sections.get(i)).getBytes("Data");

					int sectionHeight = ((CompoundTag) sections.get(i)).getByte("Y") * 16;

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
							writer.setArgb(x + cx, z + cz, colorMapping.getRGB(((block << 4) + blockData)) | 0xFF000000);
							continue zLoop;
						}
					}
				}
			}
		}
	}

	private boolean isIgnoredInNether(int biome, short block, int height) {
		if (biome == 8) {
			switch (block) {
			case 7:
			case 10:
			case 11:
			case 87:
			case 153:
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

	private int filterSections(Tag sectionA, Tag sectionB) {
		if (sectionA instanceof CompoundTag && sectionB instanceof CompoundTag) {
			CompoundTag a = (CompoundTag) sectionA;
			CompoundTag b = (CompoundTag) sectionB;
			//There are no sections with the same Y value
			return a.getByte("Y") > b.getByte("Y") ? -1 : 1;
		}
		throw new IllegalArgumentException("Can't compare non-CompoundTags");
	}
}
