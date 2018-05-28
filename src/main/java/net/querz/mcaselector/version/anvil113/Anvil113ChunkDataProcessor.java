package net.querz.mcaselector.version.anvil113;

import javafx.scene.image.PixelWriter;
import net.querz.mcaselector.ChunkDataProcessor;
import net.querz.mcaselector.ColorMapping;
import net.querz.mcaselector.tiles.Tile;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

public class Anvil113ChunkDataProcessor implements ChunkDataProcessor {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, PixelWriter writer) {
		ListTag sections = (ListTag) ((CompoundTag) root.get("Level")).get("Sections");
		sections.getValue().sort(this::filterSections);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				//loop over sections
				for (int i = 0; i < sections.size(); i++) {

					long[] blockStates = ((CompoundTag) sections.get(i)).getLongs("BlockStates");
					ListTag palette = (ListTag) ((CompoundTag) sections.get(i)).get("Palette");

					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						CompoundTag blockData = getBlockData(cx, cy, cz, blockStates, palette);
						if (!blockData.getString("Name").equals("minecraft:air") || blockData.getValue().size() > 1) {
							writer.setArgb(x + cx, z + cz, colorMapping.getRGB(blockData) | 0xFF000000);
							continue zLoop;
						}
					}
				}
			}
		}
	}

	private CompoundTag getBlockData(int x, int y, int z, long[] blockStates, ListTag palette) {
		int paletteIndex = getPaletteIndex(getIndex(x, y, z), blockStates, blockStates.length / 64);
		return palette.getCompoundTag(paletteIndex);
	}

	private int getIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}

	private int getPaletteIndex(int index, long[] blockStates, int bits) {
		double blockStatesIndex = index / (4096 / blockStates.length);

		long firstLong = blockStates[(int) blockStatesIndex];

		//calculate the bit where the index starts in this particular long value
		int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64);

		if (startBit + bits > 64) {
			//get first x bits of next long
			long secondLong = blockStates[(int) blockStatesIndex + 1];
			int first = (int) (firstLong >> startBit);
			int remainingBits = 64 - startBit;
			int last = (int) (secondLong >> remainingBits) & ((int) (Math.pow(2, remainingBits)) - 1);
			return first << remainingBits + last;
		} else {
			//           shift right               only keep <bits> amount of lsb
			return (int) (firstLong >> startBit) & ((int) Math.pow(2, bits) - 1);
		}
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
