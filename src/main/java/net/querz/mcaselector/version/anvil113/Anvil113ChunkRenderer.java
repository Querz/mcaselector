package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.math.MathUtil;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil113ChunkRenderer implements ChunkRenderer {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int height) {
		CompoundTag level = withDefault(() -> root.getCompoundTag("Level"), null);
		if (level == null) {
			return;
		}

		String status = withDefault(() -> level.getString("Status"), null);
		if (status == null || "empty".equals(status)) {
			return;
		}

		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return;
		}

		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return;
		}

		height = MathUtil.clamp(height, 0, 255);

		@SuppressWarnings("unchecked")
		ListTag<CompoundTag>[] palettes = (ListTag<CompoundTag>[]) new ListTag[16];
		long[][] blockStatesArray = new long[16][];
		sections.forEach(s -> {
			if (!s.containsKey("Palette") || !s.containsKey("BlockStates")) {
				return;
			}
			ListTag<CompoundTag> p = withDefault(() -> s.getListTag("Palette").asCompoundTagList(), null);
			int y = withDefault(() -> s.getNumber("Y").intValue(), -1);
			long[] b = withDefault(() -> s.getLongArray("BlockStates"), null);
			if (y >= 0 && y < 16 && p != null && b != null) {
				palettes[y] = p;
				blockStatesArray[y] = b;
			}
		});

		int[] biomes = withDefault(() -> level.getIntArray("Biomes"), null);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {

				int biome = getBiomeAtBlock(biomes, cx, cz);
				biome = MathUtil.clamp(biome, 0, 255);

				// loop over sections
				boolean waterDepth = false;
				for (int i = palettes.length - (16 - (height >> 4)); i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						continue;
					}

					long[] blockStates = blockStatesArray[i];
					ListTag<CompoundTag> palette = palettes[i];

					int sectionHeight = i * Tile.CHUNK_SIZE;

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);

					int startHeight;
					if (height >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - height % 16) - 1;
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.get(paletteIndex);

						if (!isEmpty(blockData)) {
							int regionIndex = (z + cz) * Tile.SIZE + (x + cx);
							if (water) {
								if (!waterDepth) {
									pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome); // water color
									waterHeights[regionIndex] = (short) (sectionHeight + cy); // height of highest water or terrain block
								}
								if (isWater(blockData)) {
									waterDepth = true;
									continue;
								} else if (isWaterlogged(blockData)) {
									pixelBuffer[regionIndex] = colorMapping.getRGB(waterDummy, biome); // water color
									waterPixels[regionIndex] = colorMapping.getRGB(blockData, biome); // color of waterlogged block
									waterHeights[regionIndex] = (short) (sectionHeight + cy);
									terrainHeights[regionIndex] = (short) (sectionHeight + cy - 1); // "height" of bottom of water, which will just be 1 block lower so shading works
									continue zLoop;
								} else {
									waterPixels[regionIndex] = colorMapping.getRGB(blockData, biome); // color of block at bottom of water
								}
							} else {
								pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
							}
							terrainHeights[regionIndex] = (short) (sectionHeight + cy); // height of bottom of water
							continue zLoop;
						}
					}
				}
			}
		}
	}

	@Override
	public void drawLayer(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, int height) {
		CompoundTag level = withDefault(() -> root.getCompoundTag("Level"), null);
		if (level == null) {
			return;
		}

		String status = withDefault(() -> level.getString("Status"), null);
		if (status == null || "empty".equals(status)) {
			return;
		}

		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return;
		}

		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return;
		}

		height = MathUtil.clamp(height, 0, 255);

		DataProperty<CompoundTag> section = new DataProperty<>();
		for (CompoundTag s : sections) {
			int y = withDefault(() -> s.getNumber("Y").intValue(), -1);
			if (y == height >> 4) {
				section.set(s);
				break;
			}
		}
		if (section.get() == null) {
			return;
		}

		if (!section.get().containsKey("Palette") || !section.get().containsKey("BlockStates")) {
			return;
		}
		ListTag<CompoundTag> palette = withDefault(() -> section.get().getListTag("Palette").asCompoundTagList(), null);
		long[] blockStates = withDefault(() -> section.get().getLongArray("BlockStates"), null);
		if (blockStates == null || palette == null) {
			return;
		}

		int[] biomes = withDefault(() -> level.getIntArray("Biomes"), null);

		int cy = height % 16;
		int bits = blockStates.length >> 6;
		int clean = ((int) Math.pow(2, bits) - 1);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
				CompoundTag blockData = palette.get(paletteIndex);

				if (isEmpty(blockData)) {
					continue;
				}

				int biome = getBiomeAtBlock(biomes, cx, cz);
				biome = MathUtil.clamp(biome, 0, 255);

				int regionIndex = (z + cz) * Tile.SIZE + (x + cx);
				pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
			}
		}
	}

	@Override
	public void drawCaves(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, short[] terrainHeights, int height) {
		CompoundTag level = withDefault(() -> root.getCompoundTag("Level"), null);
		if (level == null) {
			return;
		}

		String status = withDefault(() -> level.getString("Status"), null);
		if (status == null || "empty".equals(status)) {
			return;
		}

		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return;
		}

		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return;
		}

		height = MathUtil.clamp(height, 0, 255);

		@SuppressWarnings("unchecked")
		ListTag<CompoundTag>[] palettes = (ListTag<CompoundTag>[]) new ListTag[16];
		long[][] blockStatesArray = new long[16][];
		sections.forEach(s -> {
			if (!s.containsKey("Palette") || !s.containsKey("BlockStates")) {
				return;
			}
			ListTag<CompoundTag> p = withDefault(() -> s.getListTag("Palette").asCompoundTagList(), null);
			int y = withDefault(() -> s.getNumber("Y").intValue(), -1);
			long[] b = withDefault(() -> s.getLongArray("BlockStates"), null);
			if (y >= 0 && y < 16 && p != null && b != null) {
				palettes[y] = p;
				blockStatesArray[y] = b;
			}
		});

		int[] biomes = withDefault(() -> level.getIntArray("Biomes"), null);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {

				int ignored = 0;
				boolean doneSkipping = false;

				// loop over sections
				for (int i = palettes.length - (16 - (height >> 4)); i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						continue;
					}

					long[] blockStates = blockStatesArray[i];
					ListTag<CompoundTag> palette = palettes[i];

					int sectionHeight = i * Tile.CHUNK_SIZE;

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);

					int startHeight;
					if (height >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - height % 16) - 1;
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.get(paletteIndex);

						if (!isEmpty(blockData)) {
							if (doneSkipping) {
								int regionIndex = (z + cz) * Tile.SIZE + (x + cx);
								int biome = getBiomeAtBlock(biomes, cx, cz);
								biome = MathUtil.clamp(biome, 0, 255);
								pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
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

	private static final CompoundTag waterDummy = new CompoundTag();

	static {
		waterDummy.putString("Name", "minecraft:water");
	}

	private boolean isWater(CompoundTag blockData) {
		return switch (blockData.getString("Name")) {
			case "minecraft:water", "minecraft:bubble_column" -> true;
			default -> false;
		};
	}

	private boolean isWaterlogged(CompoundTag data) {
		return data.get("Properties") != null && "true".equals(withDefault(() -> data.getCompoundTag("Properties").getString("waterlogged"), null));
	}

	private boolean isEmpty(CompoundTag blockData) {
		return switch (blockData.getString("Name")) {
			case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void" -> blockData.size() == 1;
			default -> false;
		};
	}

	private int getIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}

	private int getBiomeIndex(int x, int z) {
		return z * Tile.CHUNK_SIZE + x;
	}

	private int getBiomeAtBlock(int[] biomes, int biomeX, int biomeZ) {
		if (biomes == null || biomes.length != 256) {
			return -1;
		}
		return biomes[getBiomeIndex(biomeX, biomeZ)];
	}

	private int getPaletteIndex(int index, long[] blockStates, int bits, int clean) {
		double blockStatesIndex = index / (4096D / blockStates.length);

		int longIndex = (int) blockStatesIndex;
		int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);

		if (startBit + bits > 64) {
			// get msb from current long, no need to cleanup manually, just fill with 0
			int previous = (int) (blockStates[longIndex] >>> startBit);

			// cleanup pattern for bits from next long
			int remainingClean = ((int) Math.pow(2, startBit + bits - 64) - 1);

			// get lsb from next long
			int next = ((int) blockStates[longIndex + 1]) & remainingClean;
			return (next << 64 - startBit) + previous;
		} else {
			return (int) (blockStates[longIndex] >> startBit) & clean;
		}
	}
}
