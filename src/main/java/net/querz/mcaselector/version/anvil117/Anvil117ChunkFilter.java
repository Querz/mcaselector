package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkFilter;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil117ChunkFilter extends Anvil113ChunkFilter {

	@Override
	public void forceBiome(CompoundTag data, int id) {
		if (data.containsKey("Level")) {
			int[] biomes = ValidationHelper.withDefault(() -> data.getCompoundTag("Level").getIntArray("Biomes"), null);
			if (biomes != null && (biomes.length == 1024 || biomes.length == 1536)) {
				biomes = new int[biomes.length];
			} else {
				biomes = new int[1536];
			}
			Arrays.fill(biomes, id);
			data.getCompoundTag("Level").putIntArray("Biomes", biomes);
		}
	}

	@Override
	public void replaceBlocks(CompoundTag data, Map<String, BlockReplaceData> replace) {
		CompoundTag level = withDefault(() -> data.getCompoundTag("Level"), null);
		if (level == null) {
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

		Point2i pos = withDefault(() -> new Point2i(level.getInt("xPos"), level.getInt("zPos")).chunkToBlock(), null);
		if (pos == null) {
			return;
		}

		// handle the special case when someone wants to replace air with something else
		if (replace.containsKey("minecraft:air")) {
			Map<Integer, CompoundTag> sectionMap = new HashMap<>();
			List<Integer> heights = new ArrayList<>(26);
			for (CompoundTag section : sections) {
				sectionMap.put((int) section.getByte("Y"), section);
				heights.add((int) section.getByte("Y"));
			}

			for (int y = -4; y < 20; y++) {
				if (!sectionMap.containsKey(y)) {
					sectionMap.put(y, createEmptySection(y));
					heights.add(y);
				} else {
					CompoundTag section = sectionMap.get(y);
					if (!section.containsKey("BlockStates") || !section.containsKey("Palette")) {
						sectionMap.put(y, createEmptySection(y));
					}
				}
			}

			heights.sort(Integer::compareTo);
			sections.clear();

			for (int height : heights) {
				sections.add(sectionMap.get(height));
			}
		}

		ListTag<CompoundTag> tileEntities = catchClassCastException(() -> level.getListTag("TileEntities").asCompoundTagList());
		if (tileEntities == null) {
			tileEntities = new ListTag<>(CompoundTag.class);
		}

		for (CompoundTag section : sections) {
			Timer p = new Timer();
			Tag<?> rawPalette = section.getListTag("Palette");
			if (rawPalette == null || rawPalette.getID() != ListTag.ID) {
				continue;
			}

			ListTag<CompoundTag> palette = catchClassCastException(((ListTag<?>) rawPalette)::asCompoundTagList);
			if (palette == null) {
				continue;
			}

			long[] blockStates = catchClassCastException(() -> section.getLongArray("BlockStates"));
			if (blockStates == null) {
				continue;
			}

			int y = section.getByte("Y");

			if (y != 3) {
				continue;
			}

			for (int i = 0; i < 4096; i++) {
				CompoundTag blockState = getBlockAt(i, blockStates, palette);
				BlockReplaceData replacement = replace.get(blockState.getString("Name"));
				if (replacement != null) {
					blockStates = setBlockAt(i, replacement.getState(), blockStates, palette);

					Point3i location = indexToLocation(i).add(pos.getX(), y * 16, pos.getZ());

					if (replacement.getTile() != null) {
						CompoundTag tile = replacement.getTile().clone();
						tile.putInt("x", location.getX());
						tile.putInt("y", location.getY());
						tile.putInt("z", location.getZ());
						tileEntities.add(tile);
					} else if (tileEntities.size() != 0) {
						for (int t = 0; t < tileEntities.size(); t++) {
							CompoundTag tile = tileEntities.get(t);
							if (tile.getInt("x") == location.getX()
									&& tile.getInt("y") == location.getY()
									&& tile.getInt("z") == location.getZ()) {
								tileEntities.remove(t);
								break;
							}
						}
					}
				}
			}

			blockStates = cleanupPalette(blockStates, palette);

			section.putLongArray("BlockStates", blockStates);

			System.out.printf("took %s to replace blocks in section %d\n", p, y);
		}

		level.put("TileEntities", tileEntities);
	}

	private Point3i indexToLocation(int i) {
		int x = i % 16;
		int z = (i - x) / 16 % 16;
		int y = (i - z * 16 - x) / 256;
		return new Point3i(x, y, z);
	}

	// returns the block state at the given index
	private CompoundTag getBlockAt(int index, long[] blockStates, ListTag<CompoundTag> palette) {
		return palette.get(getPaletteIndex(index, blockStates));
	}

	// sets a new block state at the given index.
	// if the length of blockStates changes, a new blockStates array is returned, otherwise blockStates is returned.
	private long[] setBlockAt(int index, CompoundTag blockState, long[] blockStates, ListTag<CompoundTag> palette) {
		// search palette for block and add it if necessary
		int paletteIndex = -1;
		for (int i = 0; i < palette.size(); i++) {
			if (palette.get(i).equals(blockState)) {
				paletteIndex = i;
				break;
			}
		}

		if (paletteIndex == -1) {
			palette.add(blockState);
			paletteIndex = palette.size() - 1;

			// test if we wil have to increase the blockStates array

			if ((paletteIndex & (paletteIndex - 1)) == 0) {
				blockStates = adjustBlockStateBits(palette, blockStates, null);
			}
		}

		setPaletteIndex(index, paletteIndex, blockStates);
		return blockStates;
	}

	private int getPaletteIndex(int blockIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
		double indicesPerLong = 64D / bits;
		int blockStatesIndex = (int) (blockIndex / indicesPerLong);
		int startBit = (blockIndex % (int) indicesPerLong) * bits;
		return (int) Bits.bitRange(blockStates[blockStatesIndex], startBit, startBit + bits);
	}

	private void setPaletteIndex(int blockIndex, int paletteIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
		double indicesPerLong = 64D / bits;
		int blockStatesIndex = (int) (blockIndex / indicesPerLong);
		int startBit = (blockIndex % (int) indicesPerLong) * bits;
		blockStates[blockStatesIndex] = Bits.setBits(paletteIndex, blockStates[blockStatesIndex], startBit, startBit + bits);
	}

	private long[] adjustBlockStateBits(ListTag<CompoundTag> palette, long[] blockStates, Map<Integer, Integer> oldToNewMapping) {
		int newBits = 32 - Integer.numberOfLeadingZeros(palette.size() - 1);
		newBits = Math.max(newBits, 4);

		long[] newBlockStates;
		int newLength = (int) Math.ceil(4096D / (64D / newBits));
		newBlockStates = newBits == blockStates.length / 64 ? blockStates : new long[newLength];
		if (oldToNewMapping != null) {
			for (int i = 0; i < 4096; i++) {
				setPaletteIndex(i, oldToNewMapping.get(getPaletteIndex(i, blockStates)), newBlockStates);
			}
		} else {
			for (int i = 0; i < 4096; i++) {
				setPaletteIndex(i, getPaletteIndex(i, blockStates), newBlockStates);
			}
		}
		return newBlockStates;
	}

	private long[] cleanupPalette(long[] blockStates, ListTag<CompoundTag> palette) {
		// create mapping of old --> new indices
		Map<Integer, Integer> allIndices = new HashMap<>(palette.size());
		for (int i = 0; i < 4096; i++) {
			int paletteIndex = getPaletteIndex(i, blockStates);
			allIndices.put(paletteIndex, paletteIndex);
		}

		// remove unused indices from palette
		int oldIndex = 0;
		for (int i = 0; i < palette.size(); i++) {
			if (!allIndices.containsKey(oldIndex)) {
				i--;
			} else {
				allIndices.put(oldIndex, i);
			}
			oldIndex++;
		}

		// add air to the palette if it doesn't contain air
		if (!paletteContainsAir(palette)) {
			CompoundTag air = new CompoundTag();
			air.putString("Name", "minecraft:air");
			palette.add(air);
		}

		return adjustBlockStateBits(palette, blockStates, allIndices);
	}

	private boolean paletteContainsAir(ListTag<CompoundTag> palette) {
		for (int i = 0; i < palette.size(); i++) {
			if (palette.get(i).getString("Name").equals("minecraft:air")) {
				return true;
			}
		}
		return false;
	}
}
