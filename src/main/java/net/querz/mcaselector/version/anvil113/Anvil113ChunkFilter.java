package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.mcaselector.io.registry.StatusRegistry;
import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.*;
import java.util.*;

public class Anvil113ChunkFilter implements ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, Collection<String> names) {
		ListTag sections = Helper.tagFromLevelFromRoot(data, "Sections", null);
		if (sections == null) {
			return false;
		}

		int c = 0;
		nameLoop:
		for (String name : names) {
			for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(t, "Palette", null);
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
					if (name.equals(Helper.stringFromCompound(p, "Name"))) {
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
		ListTag sections = Helper.tagFromLevelFromRoot(data, "Sections", null);
		if (sections == null) {
			return false;
		}

		for (String name : names) {
			for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(t, "Palette", null);
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
					if (name.equals(Helper.stringFromCompound(p, "Name"))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean paletteEquals(CompoundTag data, Collection<String> names) {
		ListTag sections = Helper.tagFromLevelFromRoot(data, "Sections", null);
		if (sections == null) {
			return false;
		}

		Set<String> blocks = new HashSet<>();
		for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
			ListTag palette = Helper.tagFromCompound(t, "Palette", null);
			if (palette == null) {
				continue;
			}
			for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
				String n;
				if ((n = Helper.stringFromCompound(p, "Name")) != null) {
					if (!names.contains(n)) {
						return false;
					}
					blocks.add(n);
				}
			}
		}
		if (blocks.size() != names.size()) {
			return false;
		}
		for (String name : names) {
			if (!blocks.contains(name)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean matchBiomes(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
		IntArrayTag biomesTag = Helper.tagFromLevelFromRoot(data, "Biomes", null);
		if (biomesTag == null) {
			return false;
		}

		filterLoop:
		for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
			for (int dataID : biomesTag.getValue()) {
				if (identifier.matches(dataID)) {
					continue filterLoop;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean matchAnyBiome(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
		IntArrayTag biomesTag = Helper.tagFromLevelFromRoot(data, "Biomes", null);
		if (biomesTag == null) {
			return false;
		}

		for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
			for (int dataID : biomesTag.getValue()) {
				if (identifier.matches(dataID)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void changeBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome) {
		IntArrayTag biomesTag = Helper.tagFromLevelFromRoot(data, "Biomes", null);
		if (biomesTag != null) {
			Arrays.fill(biomesTag.getValue(), biome.getID());
		}
	}

	@Override
	public void forceBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome) {
		CompoundTag level = Helper.levelFromRoot(data);
		if (level != null) {
			int[] biomes = new int[256];
			Arrays.fill(biomes, (byte) biome.getID());
			level.putIntArray("Biomes", biomes);
		}
	}

	@Override
	public void replaceBlocks(CompoundTag data, Map<String, BlockReplaceData> replace) {
		ListTag sections = Helper.tagFromLevelFromRoot(data, "Sections", null);
		if (sections == null) {
			return;
		}

		CompoundTag level = data.getCompound("Level");

		Point2i pos = Helper.point2iFromCompound(level, "xPos", "zPos");
		if (pos == null) {
			return;
		}
		pos = pos.chunkToBlock();

		// handle the special case when someone wants to replace air with something else
		if (replace.containsKey("minecraft:air")) {
			Map<Integer, CompoundTag> sectionMap = new HashMap<>();
			List<Integer> heights = new ArrayList<>(18);
			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				sectionMap.put(section.getInt("Y"), section);
				heights.add(section.getInt("Y"));
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

		ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities", null);
		if (tileEntities == null) {
			tileEntities = new ListTag();
		}

		for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
			ListTag palette = Helper.tagFromCompound(section, "Palette", null);
			if (palette == null) {
				continue;
			}

			long[] blockStates = Helper.longArrayFromCompound(section, "BlockStates");
			if (blockStates == null) {
				continue;
			}

			int y = Helper.numberFromCompound(section, "Y", -1).intValue();
			if (y < 0 || y > 15) {
				continue;
			}

			section.remove("BlockLight");
			section.remove("SkyLight");

			for (int i = 0; i < 4096; i++) {
				CompoundTag blockState = getBlockAt(i, blockStates, palette);

				for (Map.Entry<String, BlockReplaceData> entry : replace.entrySet()) {
					if (!blockState.getString("Name").matches(entry.getKey())) {
						continue;
					}
					BlockReplaceData replacement = entry.getValue();

					try {
						blockStates = setBlockAt(i, replacement.getState(), blockStates, palette);
					} catch (Exception ex) {
						throw new RuntimeException("failed to set block in section " + y, ex);
					}

					Point3i location = indexToLocation(i).add(pos.getX(), y * 16, pos.getZ());

					if (replacement.getTile() != null) {
						CompoundTag tile = replacement.getTile().copy();
						tile.putInt("x", location.getX());
						tile.putInt("y", location.getY());
						tile.putInt("z", location.getZ());
						tileEntities.add(tile);
					} else if (tileEntities.size() != 0) {
						for (int t = 0; t < tileEntities.size(); t++) {
							CompoundTag tile = tileEntities.getCompound(t);
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
	protected CompoundTag getBlockAt(int index, long[] blockStates, ListTag palette) {
		return palette.getCompound(getPaletteIndex(index, blockStates));
	}

	// sets a new block state at the given index.
	// if the length of blockStates changes, a new blockStates array is returned, otherwise blockStates is returned.
	protected long[] setBlockAt(int index, CompoundTag blockState, long[] blockStates, ListTag palette) {
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
			blockStates[longIndex + 1] = Bits.setBits(paletteIndex >> (64 - startBit), blockStates[longIndex + 1], 0, startBit + bits - 64);
		} else {
			blockStates[longIndex] = Bits.setBits(paletteIndex, blockStates[longIndex], startBit, startBit + bits);
		}
	}

	protected long[] adjustBlockStateBits(ListTag palette, long[] blockStates, Map<Integer, Integer> oldToNewMapping) {
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

	protected long[] cleanupPalette(long[] blockStates, ListTag palette) {
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

	protected boolean paletteContainsAir(ListTag palette) {
		for (int i = 0; i < palette.size(); i++) {
			if (palette.getCompound(i).getString("Name").equals("minecraft:air")) {
				return true;
			}
		}
		return false;
	}

	protected CompoundTag createEmptySection(int y) {
		CompoundTag newSection = new CompoundTag();
		newSection.putByte("Y", (byte) y);
		newSection.putLongArray("BlockStates", new long[256]);
		ListTag newPalette = new ListTag();
		CompoundTag newBlockState = new CompoundTag();
		newBlockState.putString("Name", "minecraft:air");
		newPalette.add(newBlockState);
		newSection.put("Palette", newPalette);
		return newSection;
	}

	@Override
	public int getAverageHeight(CompoundTag data) {
		ListTag sections = Helper.tagFromLevelFromRoot(data, "Sections", null);
		if (sections == null) {
			return 0;
		}

		sections.sort(this::filterSections);

		int totalHeight = 0;

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					ListTag palette = Helper.tagFromCompound(section, "Palette", null);
					if (palette == null) {
						continue;
					}

					long[] blockStates = Helper.longArrayFromCompound(section, "BlockStates");
					if (blockStates == null) {
						continue;
					}

					Number height = Helper.numberFromCompound(section, "Y", null);
					if (height == null) {
						continue;
					}

					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						int index = cy * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + cz * Tile.CHUNK_SIZE + cx;
						CompoundTag block = getBlockAt(index, blockStates, palette);
						if (!isEmpty(block)){
							totalHeight += height.intValue() * 16 + cy;
							continue zLoop;
						}
					}
				}
			}
		}
		return totalHeight / (Tile.CHUNK_SIZE * Tile.CHUNK_SIZE);
	}

	protected boolean isEmpty(CompoundTag blockData) {
		return switch (Helper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void" ->
					blockData.size() == 1;
			default -> false;
		};
	}

	protected int filterSections(Tag sectionA, Tag sectionB) {
		return Helper.numberFromCompound(sectionB, "Y", -1).intValue() - Helper.numberFromCompound(sectionA, "Y", -1).intValue();
	}

	@Override
	public int getBlockAmount(CompoundTag data, String[] blocks) {
		ListTag sections = Helper.tagFromLevelFromRoot(data, "Sections", null);
		if (sections == null) {
			return 0;
		}

		int result = 0;

		for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
			ListTag palette = Helper.tagFromCompound(section, "Palette", null);
			if (palette == null) {
				continue;
			}

			for (int i = 0; i < palette.size(); i++) {
				CompoundTag blockState = palette.getCompound(i);
				String name = Helper.stringFromCompound(blockState, "Name");
				if (name == null) {
					continue;
				}

				for (String block : blocks) {
					if (name.equals(block)) {
						// count blocks of this type

						long[] blockStates = Helper.longArrayFromCompound(section, "BlockStates");
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

	@Override
	public ListTag getTileEntities(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "TileEntities");
	}

	@Override
	public CompoundTag getStructureReferences(CompoundTag data) {
		CompoundTag structures = Helper.tagFromLevelFromRoot(data, "Structures");
		return Helper.tagFromCompound(structures, "References");
	}

	@Override
	public CompoundTag getStructureStarts(CompoundTag data) {
		CompoundTag structures = Helper.tagFromLevelFromRoot(data, "Structures");
		return Helper.tagFromCompound(structures, "Starts");
	}

	@Override
	public ListTag getSections(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "Sections");
	}

	@Override
	public void deleteSections(CompoundTag data, List<Range> ranges) {
		ListTag sections = Helper.tagFromLevelFromRoot(data, "Sections");
		if (sections == null) {
			return;
		}
		for (int i = 0; i < sections.size(); i++) {
			CompoundTag section = sections.getCompound(i);
			for (Range range : ranges) {
				if (range.contains(section.getInt("Y"))) {
					sections.remove(i);
					i--;
				}
			}
		}
	}

	@Override
	public LongTag getInhabitedTime(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "InhabitedTime");
	}

	@Override
	public void setInhabitedTime(CompoundTag data, long inhabitedTime) {
		CompoundTag level = Helper.levelFromRoot(data);
		if (level != null) {
			level.putLong("InhabitedTime", inhabitedTime);
		}
	}

	@Override
	public StringTag getStatus(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "Status");
	}

	@Override
	public void setStatus(CompoundTag data, StatusRegistry.StatusIdentifier status) {
		CompoundTag level = Helper.levelFromRoot(data);
		if (level != null) {
			level.putString("Status", status.getStatus());
		}
	}

	@Override
	public boolean matchStatus(CompoundTag data, StatusRegistry.StatusIdentifier status) {
		StringTag tag = getStatus(data);
		if (tag == null) {
			return false;
		}
		return status.getStatus().equals(tag.getValue());
	}

	@Override
	public LongTag getLastUpdate(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "LastUpdate");
	}

	@Override
	public void setLastUpdate(CompoundTag data, long lastUpdate) {
		CompoundTag level = Helper.levelFromRoot(data);
		if (level != null) {
			level.putLong("Status", lastUpdate);
		}
	}

	@Override
	public IntTag getXPos(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "xPos");
	}

	@Override
	public IntTag getYPos(CompoundTag data) {
		return null;
	}

	@Override
	public IntTag getZPos(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "zPos");
	}

	@Override
	public ByteTag getLightPopulated(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "LightPopulated");
	}

	@Override
	public void setLightPopulated(CompoundTag data, byte lightPopulated) {
		CompoundTag level = Helper.levelFromRoot(data);
		if (level != null) {
			level.putLong("LightPopulated", lightPopulated);
		}
	}

	@Override
	public void forceBlending(CompoundTag data) {
		// do nothing
	}
}
