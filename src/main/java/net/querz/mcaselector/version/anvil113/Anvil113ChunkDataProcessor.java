package net.querz.mcaselector.version.anvil113;

import javafx.scene.image.PixelWriter;
import net.querz.mcaselector.version.ChunkDataProcessor;
import net.querz.mcaselector.version.ColorMapping;
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

				byte[] biomes = ((CompoundTag) root.get("Level")).getBytes("Biomes");
				int biome = -1;
				if (biomes.length != 0) {
					biome = biomes[getIndex(cx, 0, cz)];
				}

				//loop over sections
				for (int i = 0; i < sections.size(); i++) {

					long[] blockStates = ((CompoundTag) sections.get(i)).getLongs("BlockStates");
					ListTag palette = (ListTag) ((CompoundTag) sections.get(i)).get("Palette");

					int sectionHeight = ((CompoundTag) sections.get(i)).getByte("Y") * 16;

					int bits = blockStates.length / 64;
					int clean = ((int) Math.pow(2, bits) - 1);

					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.getCompoundTag(paletteIndex);

						//ignore bedrock and netherrack until 75
						if (isIgnoredInNether(biome, blockData, sectionHeight + cy)) {
							continue;
						}

						if (!isEmpty(paletteIndex, blockData)) {
							writer.setArgb(x + cx, z + cz, colorMapping.getRGB(blockData) | 0xFF000000);
							continue zLoop;
						}
					}
				}
			}
		}
	}

	private boolean isIgnoredInNether(int biome, CompoundTag blockData, int height) {
		if (biome == 8) {
			switch (blockData.getString("Name")) {
			case "minecraft:bedrock":
			case "minecraft:flowing_lava":
			case "minecraft:lava":
			case "minecraft:netherrack":
			case "minecraft:nether_quartz_ore":
				return height > 75;
			}
		}
		return false;
	}

	private boolean isEmpty(int paletteIndex, CompoundTag blockData) {
		if (paletteIndex == 0) {
			return true;
		}
		switch (blockData.getString("Name")) {
			case "minecraft:air":
			case "minecraft:cave_air":
			case "minecraft:barrier":
			case "minecraft:structure_void":
				return blockData.getValue().size() == 1;
		}
		return false;
	}

	private int getIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}

	private int getPaletteIndex(int index, long[] blockStates, int bits, int clean) {
		double blockStatesIndex = index / (4096D / blockStates.length);

		int longIndex = (int) blockStatesIndex;
		int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);

		if (startBit + bits > 64) {
			//get msb from current long, no need to cleanup manually, just fill with 0
			int previous = (int) (blockStates[longIndex] >>> startBit);

			//cleanup pattern for bits from next long
			int remainingClean = ((int) Math.pow(2, startBit + bits - 64) - 1);

			//get lsb from next long
			int next = ((int) blockStates[longIndex + 1]) & remainingClean;
			return (next << 64 - startBit) + previous;
		} else {
			return (int) (blockStates[longIndex] >> startBit) & clean;
		}
	}

	private int filterSections(Tag sectionA, Tag sectionB) {
		if (sectionA instanceof CompoundTag && sectionB instanceof CompoundTag) {
			//There are no sections with the same Y value
			return ((CompoundTag) sectionA).getByte("Y") > ((CompoundTag) sectionB).getByte("Y") ? -1 : 1;
		}
		throw new IllegalArgumentException("Can't compare non-CompoundTags");
	}
}
