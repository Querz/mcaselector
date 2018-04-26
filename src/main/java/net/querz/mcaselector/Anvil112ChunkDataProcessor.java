package net.querz.mcaselector;

import com.sun.tools.javac.code.Attribute;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

import java.awt.image.BufferedImage;

public class Anvil112ChunkDataProcessor implements ChunkDataProcessor {
	@Override
	public BufferedImage drawImage(CompoundTag data) {
		return null;
	}

	private int[][] getTopLayer(CompoundTag data) {
		CompoundTag level = (CompoundTag) data.get("Level");
		ListTag sections = (ListTag) level.get("Sections");

		//sort highest to lowest
		sections.getValue().sort(this::filterSections);

		for (int i = 0; i < sections.size(); i++) {
			//looping through all sections

			short[] added;
			if (((CompoundTag) sections.get(i)).containsKey("Add")) {
				added = addBlocks(((CompoundTag) sections.get(i)).getBytes("Blocks"), ((CompoundTag) sections.get(i)).getBytes("Add"));
			} else {
				added = addBlocks(((CompoundTag) sections.get(i)).getBytes("Blocks"), new byte[2048]);
			}

			for (int cy = 15; cy >= 0; cy++) {
				//going through one section vertically
			}
		}

		//TODO: loop through sections by Y

		return null;
	}

	private short[] addBlocks(byte[] blocks, byte[] add) {
		short[] added = new short[blocks.length];
		for (int i = 0; i < blocks.length; i++) {
			if (i % 2 == 0) {
				//first 4 bits
				byte low = (byte) (add[i / 2] >> 4);
				added[i] = low;
			} else {
				//second 4 bits
				byte high = (byte) (add[i / 2] & 0x0F);
				added[i] = high;
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
			return a.getByte("Y") > b.getByte("Y") ? 1 : -1;
		}
		throw new IllegalArgumentException("Can't compare non-CompoundTags");
	}

}
