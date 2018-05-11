package net.querz.mcaselector.anvil112;

import net.querz.mcaselector.ChunkDataProcessor;
import net.querz.mcaselector.ColorMapping;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

import java.awt.image.BufferedImage;

public class Anvil112ChunkDataProcessor implements ChunkDataProcessor {

	@Override
	public void drawImage2(CompoundTag root, ColorMapping colorMapping, int x, int z, BufferedImage image) {
		ListTag sections = (ListTag) ((CompoundTag) root
				.get("Level"))
				.get("Sections");
		sections.getValue().sort(this::filterSections);
		//loop over x / z
		for (int cx = 0; cx < 16; cx++) {
			zLoop: for (int cz = 0; cz < 16; cz++) {
				//loop over sections
				for (int i = 0; i < sections.size(); i++) {

					byte[] blocks = ((CompoundTag) sections.get(i)).getBytes("Blocks");
					byte[] data = ((CompoundTag) sections.get(i)).getBytes("Data");

					//loop over y value in section from top to bottom
					for (int cy = 15; cy >= 0; cy--) {
						int index = getBlockIndex(cx, cy, cz);
						short block = (short) (blocks[index] & 0xFF);
						byte blockData = (byte) (index % 2 == 0 ? data[index / 2] & 0x0F : (data[index / 2]>>4)&0x0F);
						if (block != 0) {
							image.setRGB(x + cx, z + cz, colorMapping.getRGB(((block << 4) + blockData)));
							continue zLoop;
						}
					}
				}
			}
		}
	}

	private int getBlockIndex(int x, int y, int z) {
		return y * 16 * 16 + z * 16 + x;
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
