package net.querz.mcaselector.version.java_1_18;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_17.ChunkFilter_20w45a;
import net.querz.mcaselector.version.java_1_17.ChunkFilter_21w06a;
import net.querz.mcaselector.version.java_1_9.ChunkFilter_15w32a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.nbt.*;
import java.util.*;
import java.util.function.Predicate;

import static net.querz.mcaselector.util.validation.ValidationHelper.attempt;

public class ChunkFilter_21w37a {

	@MCVersionImplementation(2834)
	public static class Blocks extends ChunkFilter_21w06a.Blocks {

		@Override
		public boolean matchBlockNames(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			int c = 0;
			nameLoop:
			for (String name : names) {
				for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
					ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
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
		public boolean matchAnyBlockName(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			for (String name : names) {
				for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
					ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
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
		public void replaceBlocks(ChunkData data, Map<String, ChunkFilter.BlockReplaceData> replace) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			ListTag sections = Helper.tagFromCompound(level, "Sections");
			if (sections == null) {
				return;
			}

			Point2i pos = Helper.point2iFromCompound(level, "xPos", "zPos");
			if (pos == null) {
				return;
			}
			pos = pos.chunkToBlock();

			Range sectionRange = Helper.findSectionRange(level, sections);

			// handle the special case when someone wants to replace air with something else
			if (replace.containsKey("minecraft:air")) {
				Map<Integer, CompoundTag> sectionMap = new HashMap<>();
				List<Integer> heights = new ArrayList<>(sectionRange.num());
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					sectionMap.put(section.getInt("Y"), section);
					heights.add(section.getInt("Y"));
				}

				for (int y = sectionRange.getFrom(); y <= sectionRange.getTo(); y++) {
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

			ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities");
			if (tileEntities == null) {
				tileEntities = new ListTag();
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag blockStatesTag = section.getCompoundTag("block_states");
				if(blockStatesTag == null) continue;

				ListTag palette = Helper.tagFromCompound(blockStatesTag, "palette");
				long[] blockStates = Helper.longArrayFromCompound(blockStatesTag, "data");
				if (palette == null) {
					continue;
				}

				if (palette.size() == 1 && blockStates == null) {
					blockStates = new long[256];
				}

				int y = Helper.numberFromCompound(section, "Y", sectionRange.getFrom() - 1).intValue();
				if (!sectionRange.contains(y)) {
					continue;
				}

				section.remove("BlockLight");
				section.remove("SkyLight");

				for (int i = 0; i < 4096; i++) {
					CompoundTag blockState = getBlockAt(i, blockStates, palette);

					for (Map.Entry<String, ChunkFilter.BlockReplaceData> entry : replace.entrySet()) {
						if (!blockState.getString("Name").matches(entry.getKey())) {
							continue;
						}
						ChunkFilter.BlockReplaceData replacement = entry.getValue();

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
						} else if (!tileEntities.isEmpty()) {
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

			level.put("TileEntities", tileEntities);
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
		public int getBlockAmount(ChunkData data, String[] blocks) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return 0;
			}

			int result = 0;

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
				long[] blockStates = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
				if (palette == null || blockStates == null) {
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
		public int getAverageHeight(ChunkData data) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return 0;
			}

			sections.sort(this::filterSections);

			int totalHeight = 0;

			for (int cx = 0; cx < 16; cx++) {
				zLoop:
				for (int cz = 0; cz < 16; cz++) {
					for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
						ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
						long[] blockStates = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
						if (palette == null || blockStates == null) {
							continue;
						}

						Number height = Helper.numberFromCompound(section, "Y", null);
						if (height == null) {
							continue;
						}

						for (int cy = 15; cy >= 0; cy--) {
							int index = cy * 256 + cz * 16 + cx;
							CompoundTag block = getBlockAt(index, blockStates, palette);
							if (!isEmpty(block)) {
								totalHeight += height.intValue() * 16 + cy;
								continue zLoop;
							}
						}
					}
				}
			}
			return totalHeight / 256;
		}
	}

	@MCVersionImplementation(2834)
	public static class Heightmap extends ChunkFilter_21w06a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_18/heightmaps_21w37a.json", HeightmapConfig::load);
		}

		@Override
		protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
			ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
			if (sections == null) {
				return new long[37];
			}

			Range sectionRange = Helper.findSectionRange(root, sections);

			ListTag[] palettes = new ListTag[sectionRange.num()];
			long[][] blockStatesArray = new long[sectionRange.num()][];
			sections.forEach(s -> {
				ListTag p = Helper.tagFromCompound(s, "palette");
				long[] b = Helper.longArrayFromCompound(s, "block_states");
				int y = Helper.numberFromCompound(s, "Y", sectionRange.getFrom() - 1).intValue();
				if (sectionRange.contains(y) && p != null && b != null) {
					palettes[y - sectionRange.getFrom()] = p;
					blockStatesArray[y - sectionRange.getFrom()] = b;
				}
			});

			short[] heightmap = new short[256];

			// loop over x/z
			for (int cx = 0; cx < 16; cx++) {
				loop:
				for (int cz = 0; cz < 16; cz++) {
					for (int i = palettes.length - 1; i >= 0; i--) {
						ListTag palette = palettes[i];
						if (palette == null) {
							continue;
						}
						long[] blockStates = blockStatesArray[i];
						for (int cy = 15; cy >= 0; cy--) {
							int blockIndex = cy * 256 + cz * 16 + cx;
							if (matcher.test(getBlockAt(blockIndex, blockStates, palette))) {
								heightmap[cz * 16 + cx] = (short) (i * 16 + cy + 1);
								continue loop;
							}
						}
					}
				}
			}

			int bits = 32 - Integer.numberOfLeadingZeros(sectionRange.num() * 16);
			return applyHeightMap(heightmap, bits);
		}
	}

	@MCVersionImplementation(2834)
	public static class Palette implements ChunkFilter.Palette {

		@Override
		public boolean paletteEquals(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			Set<String> blocks = new HashSet<>();
			for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
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
	}

	@MCVersionImplementation(2834)
	public static class Biomes implements ChunkFilter.Biomes {

		@Override
		public boolean matchBiomes(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			Set<String> names = new HashSet<>(biomes.size());

			filterLoop:
			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					ListTag biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
					if (biomePalette == null) {
						continue;
					}
					for (StringTag biomeName : biomePalette.iterateType(StringTag.class)) {
						if (identifier.matches(biomeName.getValue())) {
							names.add(biomeName.getValue());
							if (biomes.size() == names.size()) {
								return true;
							}
							continue filterLoop;
						}
					}
				}
			}
			return biomes.size() == names.size();
		}

		@Override
		public boolean matchAnyBiome(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					ListTag biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
					if (biomePalette == null) {
						continue;
					}
					for (StringTag biomeName : biomePalette.iterateType(StringTag.class)) {
						if (identifier.matches(biomeName.getValue())) {
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public void changeBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return;
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag biomes = Helper.tagFromCompound(section, "biomes");
				if (biomes == null) {
					continue;
				}

				ListTag newBiomePalette = new ListTag();
				newBiomePalette.addString(biome.getName());
				biomes.put("palette", newBiomePalette);
				biomes.putLongArray("data", new long[1]);
			}
		}

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return;
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag biomes = new CompoundTag();
				ListTag newBiomePalette = new ListTag();
				newBiomePalette.addString(biome.getName());
				biomes.put("palette", newBiomePalette);
				biomes.putLongArray("data", new long[1]);
				section.put("biomes", biomes);
			}
		}
	}

	@MCVersionImplementation(2834)
	public static class Sections extends ChunkFilter_15w32a.Sections {

		@Override
		public void deleteSections(ChunkData data, List<Range> ranges) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			switch (Helper.stringFromCompound(level, "Status", "")) {
			case "light", "spawn", "heightmaps", "full" -> level.putString("Status", "features");
			default -> {return;}
			}
			ListTag sections = Helper.tagFromCompound(level, "Sections");
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
		protected void deleteSection(CompoundTag section) {
			CompoundTag blockStates = section.getCompound("block_states");
			blockStates.remove("data");
			ListTag blockPalette = new ListTag();
			CompoundTag air = new CompoundTag();
			air.putString("Name", "minecraft:air");
			blockPalette.add(air);
			blockStates.put("palette", blockPalette);
			section.remove("BlockLight");
		}
	}

	@MCVersionImplementation(2834)
	public static class RelocateEntities extends ChunkFilter_20w45a.RelocateEntities {

		@Override
		protected void applyOffsetToEntity(CompoundTag entity, Point3i offset) {
			if (entity == null) {
				return;
			}

			ListTag entityPos = Helper.tagFromCompound(entity, "Pos");
			if (entityPos != null && entityPos.size() == 3) {
				entityPos.set(0, DoubleTag.valueOf(entityPos.getDouble(0) + offset.getX()));
				entityPos.set(1, DoubleTag.valueOf(entityPos.getDouble(1) + offset.getY()));
				entityPos.set(2, DoubleTag.valueOf(entityPos.getDouble(2) + offset.getZ()));
			}

			// leashed entities
			CompoundTag leash = Helper.tagFromCompound(entity, "Leash");
			Helper.applyIntOffsetIfRootPresent(leash, "X", "Y", "Z", offset);

			// projectiles
			if (attempt(() -> Helper.applyIntOffsetIfRootPresent(entity, "xTile", "yTile", "zTile", offset))) {
				attempt(() -> Helper.applyShortOffsetIfRootPresent(entity, "xTile", "yTile", "zTile", offset));
			}

			// entities that have a sleeping place
			Helper.applyIntOffsetIfRootPresent(entity, "SleepingX", "SleepingY", "SleepingZ", offset);

			// positions for specific entity types
			String id = Helper.stringFromCompound(entity, "id", "");
			switch (id) {
			case "minecraft:dolphin":
				Helper.applyIntOffsetIfRootPresent(entity, "TreasurePosX", "TreasurePosY", "TreasurePosZ", offset);
				break;
			case "minecraft:phantom":
				Helper.applyIntOffsetIfRootPresent(entity, "AX", "AY", "AZ", offset);
				break;
			case "minecraft:shulker":
				Helper.applyIntOffsetIfRootPresent(entity, "APX", "APY", "APZ", offset);
				break;
			case "minecraft:turtle":
				Helper.applyIntOffsetIfRootPresent(entity, "HomePosX", "HomePosY", "HomePosZ", offset);
				Helper.applyIntOffsetIfRootPresent(entity, "TravelPosX", "TravelPosY", "TravelPosZ", offset);
				break;
			case "minecraft:vex":
				Helper.applyIntOffsetIfRootPresent(entity, "BoundX", "BoundY", "BoundZ", offset);
				break;
			case "minecraft:wandering_trader":
				CompoundTag wanderTarget = Helper.tagFromCompound(entity, "WanderTarget");
				Helper.applyIntOffsetIfRootPresent(wanderTarget, "X", "Y", "Z", offset);
				break;
			case "minecraft:shulker_bullet":
				CompoundTag owner = Helper.tagFromCompound(entity, "Owner");
				Helper.applyIntOffsetIfRootPresent(owner, "X", "Y", "Z", offset);
				CompoundTag target = Helper.tagFromCompound(entity, "Target");
				Helper.applyIntOffsetIfRootPresent(target, "X", "Y", "Z", offset);
				break;
			case "minecraft:end_crystal":
				CompoundTag beamTarget = Helper.tagFromCompound(entity, "BeamTarget");
				Helper.applyIntOffsetIfRootPresent(beamTarget, "X", "Y", "Z", offset);
				break;
			case "minecraft:item_frame":
			case "minecraft:painting":
				Helper.applyIntOffsetIfRootPresent(entity, "TileX", "TileY", "TileZ", offset);
				break;
			case "minecraft:villager":
				CompoundTag memories = Helper.tagFromCompound(Helper.tagFromCompound(entity, "Brain"), "memories");
				if (memories != null && !memories.isEmpty()) {
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:meeting_point"), offset);
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:home"), offset);
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:job_site"), offset);
				}
				break;
			case "minecraft:pillager":
			case "minecraft:witch":
			case "minecraft:vindicator":
			case "minecraft:ravager":
			case "minecraft:illusioner":
			case "minecraft:evoker":
				CompoundTag patrolTarget = Helper.tagFromCompound(entity, "PatrolTarget");
				Helper.applyIntOffsetIfRootPresent(patrolTarget, "X", "Y", "Z", offset);
				break;
			case "minecraft:falling_block":
				CompoundTag tileEntityData = Helper.tagFromCompound(entity, "TileEntityData");
				Relocate.instance.applyOffsetToTileEntity(tileEntityData, offset);
				break;
			case "minecraft:sniffer":
				CompoundTag snifferMemories = Helper.tagFromCompound(Helper.tagFromCompound(entity, "Brain"), "memories");
				if (snifferMemories != null && !snifferMemories.isEmpty()) {
					ListTag value = Helper.tagFromCompound(Helper.tagFromCompound(snifferMemories, "minecraft:sniffer_explored_positions"), "value");
					if (value != null && !value.isEmpty() && value.getElementType() == Tag.Type.COMPOUND) {
						for (CompoundTag v : value.iterateType(CompoundTag.class)) {
							IntArrayTag pos = v.getIntArrayTag("pos");
							if (pos != null) {
								Helper.applyOffsetToIntArrayPos(pos, offset);
							}
						}
					}
				}
				break;
			}

			// recursively update passengers
			ListTag passengers = Helper.tagFromCompound(entity, "Passengers");
			if (passengers != null) {
				passengers.forEach(p -> applyOffsetToEntity((CompoundTag) p, offset));
			}

			CompoundTag item = Helper.tagFromCompound(entity, "Item");
			Relocate.instance.applyOffsetToItem(item, offset);

			ListTag items = Helper.tagFromCompound(entity, "Items");
			if (items != null) {
				items.forEach(i -> Relocate.instance.applyOffsetToItem((CompoundTag) i, offset));
			}

			ListTag handItems = Helper.tagFromCompound(entity, "HandItems");
			if (handItems != null) {
				handItems.forEach(i -> Relocate.instance.applyOffsetToItem((CompoundTag) i, offset));
			}

			ListTag armorItems = Helper.tagFromCompound(entity, "ArmorItems");
			if (armorItems != null) {
				armorItems.forEach(i -> Relocate.instance.applyOffsetToItem((CompoundTag) i, offset));
			}

			Helper.fixEntityUUID(entity);
		}
	}

	@MCVersionImplementation(2834)
	protected static class Relocate extends ChunkFilter_21w06a.Relocate {

		static Relocate instance;

		public Relocate() {
			instance = this;
		}

		@Override
		protected void applyOffsetToVillagerMemory(CompoundTag memory, Point3i offset) {
			super.applyOffsetToVillagerMemory(memory, offset);
		}

		@Override
		protected void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
			super.applyOffsetToTileEntity(tileEntity, offset);
		}

		@Override
		protected void applyOffsetToItem(CompoundTag item, Point3i offset) {
			super.applyOffsetToItem(item, offset);
		}
	}
}
