package net.querz.mcaselector.version.anvil118;

import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.mcaselector.version.anvil117.Anvil117ChunkFilter;
import net.querz.nbt.*;
import java.util.*;

public class Anvil118ChunkFilter extends Anvil117ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, Collection<String> names) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return false;
		}

		ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
		if (sections == null) {
			return false;
		}

		int c = 0;
		nameLoop:
		for (String name : names) {
			for (CompoundTag t : sections.iterateType(CompoundTag.TYPE)) {
				ListTag palette = Snapshot118Helper.getPalette(t, dataVersion);
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette.iterateType(CompoundTag.TYPE)) {
					if (name.equals(NbtHelper.stringFromCompound(p, "Name"))) {
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
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return false;
		}

		ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
		if (sections == null) {
			return false;
		}

		for (String name : names) {
			for (CompoundTag t : sections.iterateType(CompoundTag.TYPE)) {
				ListTag palette = Snapshot118Helper.getPalette(t, dataVersion);
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette.iterateType(CompoundTag.TYPE)) {
					if (name.equals(NbtHelper.stringFromCompound(p, "Name"))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean paletteEquals(CompoundTag data, Collection<String> names) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return false;
		}

		ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
		if (sections == null) {
			return false;
		}

		Set<String> blocks = new HashSet<>();
		for (CompoundTag t : sections.iterateType(CompoundTag.TYPE)) {
			ListTag palette = Snapshot118Helper.getPalette(t, dataVersion);
			if (palette == null) {
				continue;
			}
			for (CompoundTag p : palette.iterateType(CompoundTag.TYPE)) {
				String n;
				if ((n = NbtHelper.stringFromCompound(p, "Name")) != null) {
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
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return false;
		}

		if (dataVersion >= 2834) {
			ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
			if (sections == null) {
				return false;
			}

			Set<String> names = new HashSet<>(biomes.size());

			filterLoop:
			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
					ListTag biomePalette = NbtHelper.tagFromCompound(NbtHelper.tagFromCompound(section, "biomes"), "palette");
					if (biomePalette == null) {
						continue filterLoop;
					}
					for (StringTag biomeName : biomePalette.iterateType(StringTag.TYPE)) {
						if (identifier.matches(biomeName.getValue())) {
							names.add(biomeName.getValue());
							continue filterLoop;
						}
					}
				}
			}
			return biomes.size() == names.size();
		} else {
			IntArrayTag biomesTag = NbtHelper.tagFromLevelFromRoot(data, "Biomes");
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
	}

	@Override
	public boolean matchAnyBiome(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return false;
		}

		if (dataVersion >= 2834) {
			ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
			if (sections == null) {
				return false;
			}

			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
					ListTag biomePalette = NbtHelper.tagFromCompound(NbtHelper.tagFromCompound(section, "biomes"), "palette");
					if (biomePalette == null) {
						continue;
					}
					for (StringTag biomeName : biomePalette.iterateType(StringTag.TYPE)) {
						if (identifier.matches(biomeName.getValue())) {
							return true;
						}
					}
				}
			}
			return false;
		} else {
			IntArrayTag biomesTag = NbtHelper.tagFromLevelFromRoot(data, "Biomes");
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
	}

	@Override
	public void changeBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return;
		}

		if (dataVersion >= 2834) {
			ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
			if (sections == null) {
				return;
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
				CompoundTag biomes = NbtHelper.tagFromCompound(section, "biomes");
				if (biomes == null) {
					continue;
				}

				ListTag newBiomePalette = new ListTag();
				newBiomePalette.addString(biome.getName());
				biomes.put("palette", newBiomePalette);
				biomes.putLongArray("data", new long[1]);
			}
		} else {
			IntArrayTag biomesTag = NbtHelper.tagFromLevelFromRoot(data, "Biomes", null);
			if (biomesTag != null) {
				Arrays.fill(biomesTag.getValue(), biome.getID());
			}
		}
	}

	@Override
	public void forceBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return;
		}

		if (dataVersion >= 2834) {
			ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
			if (sections == null) {
				return;
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
				CompoundTag biomes = new CompoundTag();
				ListTag newBiomePalette = new ListTag();
				newBiomePalette.addString(biome.getName());
				biomes.put("palette", newBiomePalette);
				biomes.putLongArray("data", new long[1]);
				section.put("biomes", new CompoundTag());
			}

		} else {
			CompoundTag level = NbtHelper.levelFromRoot(data);
			if (level != null) {
				int[] biomes = NbtHelper.intArrayFromCompound(level, "Biomes");
				if (biomes != null && (biomes.length == 1024 || biomes.length == 1536)) {
					biomes = new int[biomes.length];
				} else {
					biomes = new int[1024];
				}
				Arrays.fill(biomes, biome.getID());
				level.putIntArray("Biomes", biomes);
			}
		}
	}

	@Override
	public void replaceBlocks(CompoundTag data, Map<String, BlockReplaceData> replace) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return;
		}

		ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
		if (sections == null) {
			return;
		}

		if (dataVersion >= 2834) {
			Point2i pos = Snapshot118Helper.getChunkCoordinates(data, dataVersion);
			if (pos == null) {
				return;
			}
			pos = pos.chunkToBlock();

			int yMax = NbtHelper.findHighestSection(sections, -4);

			// handle the special case when someone wants to replace air with something else
			if (replace.containsKey("minecraft:air")) {
				Map<Integer, CompoundTag> sectionMap = new HashMap<>();
				List<Integer> heights = new ArrayList<>(yMax + 5);
				for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
					sectionMap.put(section.getInt("Y"), section);
					heights.add(section.getInt("Y"));
				}

				for (int y = -4; y <= yMax; y++) {
					if (!sectionMap.containsKey(y)) {
						sectionMap.put(y, completeSection(new CompoundTag(), y));
						heights.add(y);
					} else {
						CompoundTag section = sectionMap.get(y);
						if (!section.containsKey("block_states")) {
							completeSection(sectionMap.get(y), y);
						}
					}
				}

				heights.sort(Integer::compareTo);
				sections.clear();

				for (int height : heights) {
					sections.add(sectionMap.get(height));
				}
			}

			ListTag tileEntities = Snapshot118Helper.getTileEntities(data, dataVersion);
			if (tileEntities == null) {
				tileEntities = new ListTag();
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
				CompoundTag blockStatesTag = section.getCompound("block_states");
				ListTag palette = NbtHelper.tagFromCompound(blockStatesTag, "palette");
				long[] blockStates = NbtHelper.longArrayFromCompound(blockStatesTag, "data");
				if (palette == null) {
					continue;
				}

				if (palette.size() == 1 && blockStates == null) {
					blockStates = new long[256];
				}

				int y = NbtHelper.numberFromCompound(section, "Y", -5).intValue();
				if (y < -4 || y > yMax) {
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

				if (blockStates == null) {
					blockStatesTag.remove("data");
				} else {
					blockStatesTag.putLongArray("data", blockStates);
				}
			}

			Snapshot118Helper.putTileEntities(data, tileEntities, dataVersion);
		} else {
			CompoundTag level = data.getCompound("Level");

			Point2i pos = NbtHelper.point2iFromCompound(level, "xPos", "zPos");
			if (pos == null) {
				return;
			}
			pos = pos.chunkToBlock();

			// handle the special case when someone wants to replace air with something else
			if (replace.containsKey("minecraft:air")) {
				Map<Integer, CompoundTag> sectionMap = new HashMap<>();
				List<Integer> heights = new ArrayList<>(18);
				for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
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

			ListTag tileEntities = NbtHelper.tagFromCompound(level, "TileEntities", null);
			if (tileEntities == null) {
				tileEntities = new ListTag();
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
				ListTag palette = NbtHelper.tagFromCompound(section, "Palette", null);
				if (palette == null) {
					continue;
				}

				long[] blockStates = NbtHelper.longArrayFromCompound(section, "BlockStates");
				if (blockStates == null) {
					continue;
				}

				int y = NbtHelper.numberFromCompound(section, "Y", -1).intValue();
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
	}

	@Override
	protected long[] adjustBlockStateBits(ListTag palette, long[] blockStates, Map<Integer, Integer> oldToNewMapping) {
		if (palette.size() == 1) {
			return null;
		}
		return super.adjustBlockStateBits(palette, blockStates, oldToNewMapping);
	}

	@Override
	protected int getPaletteIndex(int blockIndex, long[] blockStates) {
		if (blockStates == null) {
			return 0;
		}
		return super.getPaletteIndex(blockIndex, blockStates);
	}

	protected CompoundTag completeSection(CompoundTag section, int y) {
		section.putByte("Y", (byte) y);
		if (!section.containsKey("block_states")) {
			CompoundTag newBlockStates = new CompoundTag();
			section.put("block_states", newBlockStates);
		}
		CompoundTag blockStates = section.getCompound("block_states");

		if (!blockStates.containsKey("data")) {
			blockStates.putLongArray("data", new long[256]);
		}
		if (!blockStates.containsKey("palette")) {
			ListTag newPalette = new ListTag();
			CompoundTag newBlockState = new CompoundTag();
			newBlockState.putString("Name", "minecraft:air");
			newPalette.add(newBlockState);
			blockStates.put("palette", newPalette);
		}

		if (!section.containsKey("biomes")) {
			CompoundTag newBiomes = new CompoundTag();
			section.put("biomes", newBiomes);
		}
		CompoundTag biomes = section.getCompound("biomes");

		if (!biomes.containsKey("palette")) {
			ListTag biomePalette = new ListTag();
			biomePalette.addString("minecraft:plains");
			biomes.put("palette", biomePalette);
		}
		if (!biomes.containsKey("data")) {
			biomes.putLongArray("data", new long[1]);
		}
		return section;
	}

	@Override
	public int getAverageHeight(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return 0;
		}

		ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
		if (sections == null) {
			return 0;
		}

		sections.sort(this::filterSections);

		int totalHeight = 0;

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
					ListTag palette = Snapshot118Helper.getPalette(section, dataVersion);
					long[] blockStates = Snapshot118Helper.getBlockStates(section, dataVersion);
					if (palette == null) {
						continue;
					}

					Number height = NbtHelper.numberFromCompound(section, "Y", null);
					if (height == null) {
						continue;
					}

					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						int index = cy * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + cz * Tile.CHUNK_SIZE + cx;
						CompoundTag block = getBlockAt(index, blockStates, palette);
						if (!isEmpty(block)) {
							totalHeight += height.intValue() * 16 + cy;
							continue zLoop;
						}
					}
				}
			}
		}
		return totalHeight / (Tile.CHUNK_SIZE * Tile.CHUNK_SIZE);
	}

	@Override
	public int getBlockAmount(CompoundTag data, String[] blocks) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return 0;
		}

		ListTag sections = Snapshot118Helper.getSections(data, dataVersion);
		if (sections == null) {
			return 0;
		}

		int result = 0;

		for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
			ListTag palette = Snapshot118Helper.getPalette(section, dataVersion);
			long[] blockStates = Snapshot118Helper.getBlockStates(section, dataVersion);
			if (palette == null) {
				continue;
			}

			for (int i = 0; i < palette.size(); i++) {
				CompoundTag blockState = palette.getCompound(i);
				String name = NbtHelper.stringFromCompound(blockState, "Name");
				if (name == null) {
					continue;
				}

				for (String block : blocks) {
					if (name.equals(block)) {
						// count blocks of this type
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
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getTileEntities(data, dataVersion);
	}

	@Override
	public CompoundTag getStructureReferences(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		CompoundTag structures = Snapshot118Helper.getStructures(data, dataVersion);
		return NbtHelper.tagFromCompound(structures, "References");
	}

	@Override
	public CompoundTag getStructureStarts(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getStructureStarts(data, dataVersion);
	}

	@Override
	public ListTag getSections(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getSections(data, dataVersion);
	}

	@Override
	public void deleteSections(CompoundTag data, List<Range> ranges) {
		switch (data.getString("Status")) {
			case "light":
			case "spawn":
			case "heightmaps":
			case "full":
				// need to reset the Status to pre-light to prevent the game to crash
				// when it attempts to calculate light based on missing data
				data.putString("Status", "features");
				break;
			default:
				return;
		}
		ListTag sections = NbtHelper.tagFromCompound(data, "sections");
		if (sections == null) {
			return;
		}
		for (int i = 0; i < sections.size(); i++) {
			CompoundTag section = sections.getCompound(i);
			for (Range range : ranges) {
				if (range.contains(section.getInt("Y"))) {
					deleteSection(section);
				}
			}
		}
	}

	// only delete blocks, not biomes
	private void deleteSection(CompoundTag section) {
		CompoundTag blockStates = section.getCompound("block_states");
		blockStates.remove("data");
		ListTag blockPalette = new ListTag();
		CompoundTag air = new CompoundTag();
		air.putString("Name", "minecraft:air");
		blockPalette.add(air);
		blockStates.put("palette", blockPalette);
		section.remove("BlockLight");
	}

	@Override
	public LongTag getInhabitedTime(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getInhabitedTime(data, dataVersion);
	}

	@Override
	public void setInhabitedTime(CompoundTag data, long inhabitedTime) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return;
		}
		Snapshot118Helper.setInhabitedTime(data, inhabitedTime, dataVersion);
	}

	@Override
	public StringTag getStatus(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getStatus(data, dataVersion);
	}

	@Override
	public void setStatus(CompoundTag data, String status) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return;
		}
		Snapshot118Helper.setStatus(data, status, dataVersion);
	}

	@Override
	public LongTag getLastUpdate(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getLastUpdate(data, dataVersion);
	}

	@Override
	public void setLastUpdate(CompoundTag data, long lastUpdate) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return;
		}
		Snapshot118Helper.setLastUpdate(data, lastUpdate, dataVersion);
	}

	@Override
	public IntTag getXPos(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getXPos(data, dataVersion);
	}

	@Override
	public IntTag getYPos(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getYPos(data, dataVersion);
	}

	@Override
	public IntTag getZPos(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getZPos(data, dataVersion);
	}

	@Override
	public ByteTag getLightPopulated(CompoundTag data) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return null;
		}
		return Snapshot118Helper.getIsLightOn(data, dataVersion);
	}

	@Override
	public void setLightPopulated(CompoundTag data, byte lightPopulated) {
		Integer dataVersion = NbtHelper.intFromCompound(data, "DataVersion");
		if (dataVersion == null) {
			return;
		}
		Snapshot118Helper.setIsLightOn(data, lightPopulated, dataVersion);
	}

	@Override
	public void forceBlending(CompoundTag data) {
		CompoundTag blendingData = new CompoundTag();
		blendingData.putByte("old_noise", (byte) 1);
		data.put("blending_data", blendingData);
		data.remove("Heightmaps");
		data.remove("isLightOn");
	}
}
