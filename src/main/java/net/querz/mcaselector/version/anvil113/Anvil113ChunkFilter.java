package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil113ChunkFilter implements ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, Collection<String> names) {
		CompoundTag level = withDefault(() -> data.getCompoundTag("Level"), null);
		if (level == null) {
			return false;
		}
		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return false;
		}
		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return false;
		}
		int c = 0;
		nameLoop:
		for (String name : names) {
			for (CompoundTag t : sections) {
				ListTag<?> rawPalette = withDefault(() -> t.getListTag("Palette"), null);
				if (rawPalette == null) {
					continue;
				}
				ListTag<CompoundTag> palette = catchClassCastException(rawPalette::asCompoundTagList);
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette) {
					if (name.equals(withDefault(() -> p.getString("Name"), null))) {
						c++;
						continue nameLoop;
					}
				}
			}
		}
		return names.size() == c;
	}

	@Override
	public boolean matchAnyBlockName(CompoundTag data, Collection<String> names) {
		CompoundTag level = withDefault(() -> data.getCompoundTag("Level"), null);
		if (level == null) {
			return false;
		}
		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return false;
		}
		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return false;
		}
		for (String name : names) {
			for (CompoundTag t : sections) {
				ListTag<?> rawPalette = withDefault(() -> t.getListTag("Palette"), null);
				if (rawPalette == null) {
					continue;
				}
				ListTag<CompoundTag> palette = catchClassCastException(rawPalette::asCompoundTagList);
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette) {
					if (name.equals(withDefault(() -> p.getString("Name"), null))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean matchBiomeIDs(CompoundTag data, Collection<Integer> ids) {
		if (!data.containsKey("Level") || withDefault(() -> data.getCompoundTag("Level").getIntArrayTag("Biomes"), null) == null) {
			return false;
		}

		filterLoop:
		for (int filterID : ids) {
			for (int dataID : data.getCompoundTag("Level").getIntArray("Biomes")) {
				if (filterID == dataID) {
					continue filterLoop;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean matchAnyBiomeID(CompoundTag data, Collection<Integer> ids) {
		if (!data.containsKey("Level") || withDefault(() -> data.getCompoundTag("Level").getIntArrayTag("Biomes"), null) == null) {
			return false;
		}

		for (int filterID : ids) {
			for (int dataID : data.getCompoundTag("Level").getIntArray("Biomes")) {
				if (filterID == dataID) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void changeBiome(CompoundTag data, int id) {
		if (!data.containsKey("Level") || withDefault(() -> data.getCompoundTag("Level").getIntArrayTag("Biomes"), null) == null) {
			return;
		}
		Arrays.fill(data.getCompoundTag("Level").getIntArray("Biomes"), id);
	}

	@Override
	public void forceBiome(CompoundTag data, int id) {
		if (data.containsKey("Level")) {
			int[] biomes = new int[256];
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
			List<Integer> heights = new ArrayList<>(18);
			for (CompoundTag section : sections) {
				sectionMap.put(section.getNumber("Y").intValue(), section);
				heights.add(section.getNumber("Y").intValue());
			}

			for (int y = 0; y < 16; y++) {
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

			for (int i = 0; i < 4096; i++) {
				CompoundTag blockState = getBlockAt(i, blockStates, palette);

				BlockReplaceData replacement = replace.get(blockState.getString("Name"));
				if (replacement != null) {

					try {
						blockStates = setBlockAt(i, replacement.getState(), blockStates, palette);
					} catch (Exception ex) {
						throw new RuntimeException("failed to set block in section " + y, ex);
					}

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

			try {
				blockStates = cleanupPalette(blockStates, palette);
			} catch (Exception ex) {
				throw new RuntimeException("failed to cleanup section " + y, ex);
			}

			section.putLongArray("BlockStates", blockStates);
		}

		level.put("TileEntities", tileEntities);
	}

	protected Point3i indexToLocation(int i) {
		int x = i % 16;
		int z = (i - x) / 16 % 16;
		int y = (i - z * 16 - x) / 256;
		return new Point3i(x, y, z);
	}

	// returns the block state at the given index
	protected CompoundTag getBlockAt(int index, long[] blockStates, ListTag<CompoundTag> palette) {
		return palette.get(getPaletteIndex(index, blockStates));
	}

	// sets a new block state at the given index.
	// if the length of blockStates changes, a new blockStates array is returned, otherwise blockStates is returned.
	protected long[] setBlockAt(int index, CompoundTag blockState, long[] blockStates, ListTag<CompoundTag> palette) {
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

	protected int getPaletteIndex(int blockIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
		double blockStatesIndex = blockIndex / (4096D / blockStates.length);
		int longIndex = (int) blockStatesIndex;
		int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);
		if (startBit + bits > 64) {
			long prev = Bits.bitRange(blockStates[longIndex], startBit, 64);
			long next = Bits.bitRange(blockStates[longIndex + 1], 0, startBit + bits - 64);
			return (int) ((next << 64 - startBit) + prev);
		} else {
			return (int) Bits.bitRange(blockStates[longIndex], startBit, startBit + bits);
		}
	}

	protected void setPaletteIndex(int blockIndex, int paletteIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
		double blockStatesIndex = blockIndex / (4096D / blockStates.length);
		int longIndex = (int) blockStatesIndex;
		int startBit = (int) ((blockStatesIndex - Math.floor(longIndex)) * 64D);
		if (startBit + bits > 64) {
			blockStates[longIndex] = Bits.setBits(paletteIndex, blockStates[longIndex], startBit, 64);
			blockStates[longIndex + 1] = Bits.setBits(paletteIndex, blockStates[longIndex + 1], startBit - 64, startBit + bits - 64);
		} else {
			blockStates[longIndex] = Bits.setBits(paletteIndex, blockStates[longIndex], startBit, startBit + bits);
		}
	}

	protected long[] adjustBlockStateBits(ListTag<CompoundTag> palette, long[] blockStates, Map<Integer, Integer> oldToNewMapping) {
		int newBits = 32 - Integer.numberOfLeadingZeros(palette.size() - 1);
		newBits = Math.max(newBits, 4);

		long[] newBlockStates;
		if (newBits == blockStates.length / 64) {
			newBlockStates = blockStates;
		} else {
			newBlockStates = new long[newBits * 64];
		}

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

	protected long[] cleanupPalette(long[] blockStates, ListTag<CompoundTag> palette) {
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
				palette.remove(i);
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

	protected boolean paletteContainsAir(ListTag<CompoundTag> palette) {
		for (int i = 0; i < palette.size(); i++) {
			if (palette.get(i).getString("Name").equals("minecraft:air")) {
				return true;
			}
		}
		return false;
	}

	protected CompoundTag createEmptySection(int y) {
		CompoundTag newSection = new CompoundTag();
		newSection.putByte("Y", (byte) y);
		newSection.putLongArray("BlockStates", new long[256]);
		ListTag<CompoundTag> newPalette = new ListTag<>(CompoundTag.class);
		CompoundTag newBlockState = new CompoundTag();
		newBlockState.putString("Name", "minecraft:air");
		newPalette.add(newBlockState);
		newSection.put("Palette", newPalette);
		return newSection;
	}

	@Override
	public int getAverageHeight(CompoundTag data) {
		CompoundTag level = withDefault(() -> data.getCompoundTag("Level"), null);
		if (level == null) {
			return 0;
		}
		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return 0;
		}
		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return 0;
		}

		sections.sort(this::filterSections);

		int totalHeight = 0;

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				for (int i = 0; i < sections.size(); i++) {
					CompoundTag section;
					ListTag<?> rawPalette;
					ListTag<CompoundTag> palette;
					if ((section = sections.get(i)) == null
							|| (rawPalette = section.getListTag("Palette")) == null
							|| (palette = rawPalette.asCompoundTagList()) == null) {
						continue;
					}
					long[] blockStates = withDefault(() -> section.getLongArray("BlockStates"), null);
					if (blockStates == null) {
						continue;
					}

					Byte height = withDefault(() -> section.getByte("Y"), null);
					if (height == null) {
						continue;
					}

					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						int index = cy * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + cz * Tile.CHUNK_SIZE + cx;
						CompoundTag block = getBlockAt(index, blockStates, palette);
						if (!isEmpty(block)){
							totalHeight += height * 16 + cy;
							continue zLoop;
						}
					}
				}
			}
		}
		return totalHeight / (Tile.CHUNK_SIZE * Tile.CHUNK_SIZE);
	}

	protected boolean isEmpty(CompoundTag blockData) {
		switch (withDefault(() -> blockData.getString("Name"), "")) {
			case "minecraft:air":
			case "minecraft:cave_air":
			case "minecraft:barrier":
			case "minecraft:structure_void":
				return blockData.size() == 1;
		}
		return false;
	}

	protected int filterSections(CompoundTag sectionA, CompoundTag sectionB) {
		return withDefault(() -> sectionB.getNumber("Y").intValue(), -1) - withDefault(() -> sectionA.getNumber("Y").intValue(), -1);
	}

	@Override
	public int getBlockAmount(CompoundTag data, String[] blocks) {
		CompoundTag level = withDefault(() -> data.getCompoundTag("Level"), null);
		if (level == null) {
			return 0;
		}
		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return 0;
		}
		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return 0;
		}

		int result = 0;

		for (CompoundTag section : sections) {
			ListTag<CompoundTag> palette = ValidationHelper.withDefaultSilent(() -> section.getListTag("Palette").asCompoundTagList(), null);
			if (palette == null) {
				continue;
			}

			for (int i = 0; i < palette.size(); i++) {
				CompoundTag blockState = palette.get(i);
				String name = ValidationHelper.withDefaultSilent(() -> blockState.getString("Name"), null);
				if (name == null) {
					continue;
				}

				for (String block : blocks) {
					if (name.equals(block)) {
						// count blocks of this type

						long[] blockStates = withDefault(() -> section.getLongArray("BlockStates"), null);
						if (blockStates == null) {
							break;
						}

						for (int k = 0; k < 4096; k++) {
							if (blockState == getBlockAt(k, blockStates, palette)) {
								result++;
							}
						}
						break;
					}
				}
			}
		}
		return result;
	}
}
