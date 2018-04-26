package net.querz.mcaselector;

import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

import java.awt.image.BufferedImage;

public class Anvil112ChunkDataProcessor implements ChunkDataProcessor {
	@Override
	public BufferedImage drawImage(CompoundTag data, ColorMapping colorMapping) {
		short[][] top = getTopLayer(data);
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < top.length; x++) {
			for (int z = 0; z < top[x].length; z++) {
				image.setRGB(x, z, colorMapping.getRGB(top[x][z]));
			}
		}
		return image;
	}

	private short[][] getTopLayer(CompoundTag data) {
		short[][] top = new short[16][16];

		if (data == null) {
			return top;
		}


		CompoundTag level = (CompoundTag) data.get("Level");
		ListTag sections = (ListTag) level.get("Sections");

		//sort highest to lowest
		sections.getValue().sort(this::filterSections);

		int found = 0;
		search: for (int i = 0; i < sections.size(); i++) {
			//looping through all sections

			byte[] blocks = ((CompoundTag) sections.get(i)).getBytes("Blocks");
			byte[] extras = ((CompoundTag) sections.get(i)).getBytes("Data");

			for (int cx = 0; cx < 16; cx++) {
				for (int cz = 0; cz < 16; cz++) {

					if (top[cx][cz] != 0) {
						continue;
					}

					for (int cy = 15; cy >= 0; cy--) {
						int index = getBlockIndex(cx, cy, cz);
						short block = (short) (blocks[index] & 0xFF);
						byte extra = (byte) (index % 2 == 0 ? extras[index / 2] & 0x0F : (extras[index / 2]>>4)&0x0F);
						if (block != 0) {
							top[cx][cz] = (short) ((block << 4) + extra);
							found++;
							if (found == 256) {
								break search;
							}
							break;
						}
					}
				}
			}
		}
		return top;
	}

	private int getBlockIndex(int x, int y, int z) {
		return y * 16 * 16 + z * 16 + x;
	}

	private short[] addBlocks(byte[] blocks, byte[] add) {
		short[] added = new short[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			if (i % 2 == 0) {
				//first 4 bits
				byte high = (byte) (add[i / 2] >> 4);
				added[i] = high;
			} else {
				//second 4 bits
				byte low = (byte) (add[i / 2] & 0x0F);
				added[i] = low;
			}
			added[i] <<= 8;
			added[i] |= blocks[i];
		}
		return added;
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
