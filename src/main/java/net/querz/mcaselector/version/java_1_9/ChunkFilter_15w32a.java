package net.querz.mcaselector.version.java_1_9;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static net.querz.mcaselector.util.validation.ValidationHelper.*;

public class ChunkFilter_15w32a {

	private static final Logger LOGGER = LogManager.getLogger(ChunkFilter_15w32a.class);

	private static final Map<String, BlockData[]> mapping = FileHelper.loadFromResource("mapping/java_1_9/block_name_to_id.csv", r -> {
		Map<String, BlockData[]> map = new HashMap<>();
		try (Stream<String> lines = r.lines()) {
			lines.forEach(line -> {
				String[] split = line.split(";");
				int id = Integer.parseInt(split[1]);
				String[] bytes;
				Set<Byte> data = new HashSet<>();
				if (split.length == 2 || (bytes = split[2].split(",")).length == 0) {
					for (int i = 0; i < 16; i++) {
						data.add((byte) i);
					}
				} else {
					for (String b : bytes) {
						data.add(Byte.parseByte(b));
					}
				}

				for (String name : split[0].split(",")) {
					String fullName = "minecraft:" + name;
					BlockData blockData = new BlockData(id, data);
					map.compute(fullName, (k, v) -> {
						if (v == null) {
							return new BlockData[] {blockData};
						} else {
							BlockData[] newArray = new BlockData[v.length + 1];
							System.arraycopy(v, 0, newArray, 0, v.length);
							newArray[newArray.length - 1] = blockData;
							return newArray;
						}
					});
				}
			});
		}
		return map;
	});

	private static class BlockData {
		int id;
		Set<Byte> data;

		BlockData(int id, Set<Byte> data) {
			this.id = id;
			this.data = data;
		}

		@Override
		public String toString() {
			return "{" + id + ":" + Arrays.toString(data.toArray()) + "}";
		}
	}

	private static class Block {
		int id;
		byte data;

		Block(int id, byte data) {
			this.id = id;
			this.data = data;
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, data);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof Block && ((Block) other).id == id && ((Block) other).data == data;
		}
	}

	@MCVersionImplementation(100)
	public static class Biomes implements ChunkFilter.Biomes {

		@Override
		public boolean matchBiomes(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			ByteArrayTag biomesTag = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Biomes");
			if (biomesTag == null) {
				return false;
			}

			filterLoop:
			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (byte dataID : biomesTag.getValue()) {
					if (identifier.matches(dataID)) {
						continue filterLoop;
					}
				}
				return false;
			}
			return true;
		}

		@Override
		public boolean matchAnyBiome(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			ByteArrayTag biomesTag = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Biomes");
			if (biomesTag == null) {
				return false;
			}

			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (byte dataID : biomesTag.getValue()) {
					if (identifier.matches(dataID)) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public void changeBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			ByteArrayTag biomesTag = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Biomes");
			if (biomesTag != null) {
				Arrays.fill(biomesTag.getValue(), (byte) biome.getID());
			}
		}

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				byte[] biomes = new byte[256];
				Arrays.fill(biomes, (byte) biome.getID());
				level.putByteArray("Biomes", biomes);
			}
		}
	}

	@MCVersionImplementation(100)
	public static class Blocks implements ChunkFilter.Blocks {

		@Override
		public boolean matchBlockNames(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}
			int c = 0;
			nameLoop:
			for (String name : names) {
				BlockData[] bd = mapping.get(name);
				if (bd == null) {
					LOGGER.debug("no mapping found for {}", name);
					continue;
				}
				for (Tag t : sections) {
					byte[] blocks = Helper.byteArrayFromCompound(t, "Blocks");
					if (blocks == null) {
						continue;
					}
					byte[] blockData = Helper.byteArrayFromCompound(t, "Data");
					if (blockData == null) {
						continue;
					}

					for (int i = 0; i < blocks.length; i++) {
						short b = (short) (blocks[i] & 0xFF);
						for (BlockData d : bd) {
							if (d.id == b) {
								byte dataByte = (byte) (i % 2 == 0 ? blockData[i / 2] & 0x0F : (blockData[i / 2] >> 4) & 0x0F);
								if (d.data.contains(dataByte)) {
									c++;
									continue nameLoop;
								}
							}
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
				BlockData[] bd = mapping.get(name);
				if (bd == null) {
					LOGGER.debug("no mapping found for {}", name);
					continue;
				}
				for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
					byte[] blocks = Helper.byteArrayFromCompound(t, "Blocks");
					if (blocks == null) {
						continue;
					}
					byte[] blockData = Helper.byteArrayFromCompound(t, "Data");
					if (blockData == null) {
						continue;
					}

					for (int i = 0; i < blocks.length; i++) {
						short b = (short) (blocks[i] & 0xFF);
						for (BlockData d : bd) {
							if (d.id == b) {
								byte dataByte = (byte) (i % 2 == 0 ? blockData[i / 2] & 0x0F : (blockData[i / 2] >> 4) & 0x0F);
								if (d.data.contains(dataByte)) {
									return true;
								}
							}
						}
					}
				}
			}
			return false;
		}

		@Override
		public void replaceBlocks(ChunkData data, Map<String, ChunkFilter.BlockReplaceData> replace) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return;
			}

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
						if (!section.containsKey("Blocks") || !section.containsKey("Data")) {
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

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				for (Map.Entry<String, ChunkFilter.BlockReplaceData> entry : replace.entrySet()) {
					BlockData[] bd = mapping.get(entry.getKey());
					BlockData bdr = mapping.get(entry.getValue().getName())[0];

					byte[] blocks = section.getByteArray("Blocks");
					byte[] blockData = section.getByteArray("Data");

					section.remove("BlockLight");
					section.remove("SkyLight");

					blockLoop:
					for (int i = 0; i < blocks.length; i++) {
						byte dataByte = blockData[i / 2];
						byte dataBits = (byte) (i % 2 == 0 ? dataByte & 0x0F : (dataByte >> 4) & 0x0F);
						for (BlockData d : bd) {
							if (d.id == (blocks[i] & 0xFF) && d.data.contains(dataBits)) {
								blocks[i] = (byte) bdr.id;
								byte newDataBits = bdr.data.iterator().next();
								blockData[i / 2] = (byte) (i % 2 == 0 ? (dataByte & 0xF0) + newDataBits : (dataByte & 0x0F) + (newDataBits << 4));
								continue blockLoop;
							}
						}
					}
				}
			}

			// delete tile entities with that name
			ListTag tileEntities = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "TileEntities");
			if (tileEntities != null) {
				for (int i = 0; i < tileEntities.size(); i++) {
					CompoundTag tileEntity = tileEntities.getCompound(i);
					String id = Helper.stringFromCompound(tileEntity, "id");
					if (id != null && replace.containsKey(id)) {
						tileEntities.remove(i);
						i--;
					}
				}
			}
		}

		private CompoundTag createEmptySection(int y) {
			CompoundTag newSection = new CompoundTag();
			newSection.putByte("Y", (byte) y);
			newSection.putByteArray("Blocks", new byte[4096]);
			newSection.putByteArray("Data", new byte[2048]);
			return newSection;
		}

		@Override
		public int getBlockAmount(ChunkData data, String[] blocks) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return 0;
			}

			int result = 0;

			// map block names to block ids
			for (String blockName : blocks) {
				if (!mapping.containsKey(blockName)) {
					continue;
				}

				BlockData[] blockData = mapping.get(blockName);

				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					byte[] blockIDs = Helper.byteArrayFromCompound(section, "Blocks");
					if (blockIDs == null) {
						continue;
					}

					byte[] blockIDsData = Helper.byteArrayFromCompound(section, "Data");
					if (blockIDsData == null) {
						continue;
					}

					for (int i = 0; i < blockIDs.length; i++) {
						int blockID = blockIDs[i] & 0xFF;
						byte dataByte = blockIDsData[i / 2];
						byte dataBits = (byte) (i % 2 == 0 ? dataByte & 0x0F : (dataByte >> 4) & 0x0F);
						for (BlockData bd : blockData) {
							if (blockID == bd.id && bd.data.contains(dataBits)) {
								result++;
							}
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
						byte[] blocks = Helper.byteArrayFromCompound(section, "Blocks");
						if (blocks == null) {
							continue;
						}

						Number height = Helper.numberFromCompound(section, "Y", null);
						if (height == null) {
							continue;
						}

						for (int cy = 16 - 1; cy >= 0; cy--) {
							int index = cy * 256 + cz * 16 + cx;
							if (!isEmpty(blocks[index])) {
								totalHeight += height.intValue() * 16 + cy;
								continue zLoop;
							}
						}
					}
				}
			}
			return totalHeight / 256;
		}

		private int filterSections(Tag sectionA, Tag sectionB) {
			return Helper.numberFromCompound(sectionB, "Y", -1).intValue() - Helper.numberFromCompound(sectionA, "Y", -1).intValue();
		}

		private boolean isEmpty(int blockID) {
			return blockID == 0 || blockID == 166 || blockID == 217;
		}
	}

	@MCVersionImplementation(100)
	public static class Palette implements ChunkFilter.Palette {

		@Override
		public boolean paletteEquals(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			Set<Block> blocks = new HashSet<>();
			List<BlockData> blockData = new ArrayList<>(names.size());
			for (String name : names) {
				BlockData[] bd = mapping.get(name);
				if (bd == null) {
					LOGGER.debug("no mapping found for {}", name);
					continue;
				}
				blockData.addAll(Arrays.asList(bd));
			}

			for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
				byte[] blockBytes = Helper.byteArrayFromCompound(t, "Blocks");
				if (blockBytes == null) {
					continue;
				}
				byte[] dataBits = Helper.byteArrayFromCompound(t, "Data");
				if (dataBits == null) {
					continue;
				}

				blockLoop:
				for (int i = 0; i < blockBytes.length; i++) {
					short b = (short) (blockBytes[i] & 0xFF);
					for (BlockData d : blockData) {
						if (b == d.id) {
							byte dataByte = (byte) (i % 2 == 0 ? dataBits[i / 2] & 0x0F : (dataBits[i / 2] >> 4) & 0x0F);
							if (d.data.contains(dataByte)) {
								blocks.add(new Block(b, dataByte));
								continue blockLoop;
							}
						}
					}
					// there's a block in this chunk that we are not searching for, so we return right now
					return false;
				}
			}

			blockDataLoop:
			for (BlockData bd : blockData) {
				for (Block block : blocks) {
					if (bd.id == block.id && bd.data.contains(block.data)) {
						continue blockDataLoop;
					}
				}
				// blockData contains a block that does not exist in blocks (a block does not exist in this chunk)
				return false;
			}

			return true;
		}
	}

	@MCVersionImplementation(100)
	public static class TileEntities implements ChunkFilter.TileEntities {

		@Override
		public ListTag getTileEntities(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "TileEntities");
		}
	}

	@MCVersionImplementation(100)
	public static class Sections implements ChunkFilter.Sections {

		@Override
		public ListTag getSections(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
		}

		@Override
		public void deleteSections(ChunkData data, List<Range> ranges) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
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
	}

	@MCVersionImplementation(100)
	public static class InhabitedTime implements ChunkFilter.InhabitedTime {

		@Override
		public LongTag getInhabitedTime(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "InhabitedTime");
		}

		@Override
		public void setInhabitedTime(ChunkData data, long inhabitedTime) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				level.putLong("InhabitedTime", inhabitedTime);
			}
		}
	}

	@MCVersionImplementation(100)
	public static class Status implements ChunkFilter.Status {

		@Override
		public StringTag getStatus(ChunkData data) {
			// nothing to do until 18w06a
			return null;
		}

		@Override
		public void setStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			// nothing to do until 18w06a
		}

		@Override
		public boolean matchStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			// nothing to do until 18w06a
			return false;
		}
	}

	@MCVersionImplementation(100)
	public static class LastUpdate implements ChunkFilter.LastUpdate {

		@Override
		public LongTag getLastUpdate(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "LastUpdate");
		}

		@Override
		public void setLastUpdate(ChunkData data, long lastUpdate) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				level.putLong("LastUpdate", lastUpdate);
			}
		}
	}

	@MCVersionImplementation(100)
	public static class Pos implements ChunkFilter.Pos {

		@Override
		public IntTag getXPos(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "xPos");
		}

		@Override
		public IntTag getYPos(ChunkData data) {
			return null;
		}

		@Override
		public IntTag getZPos(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "zPos");
		}
	}

	@MCVersionImplementation(100)
	public static class LightPopulated implements ChunkFilter.LightPopulated {

		@Override
		public ByteTag getLightPopulated(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "LightPopulated");
		}

		@Override
		public void setLightPopulated(ChunkData data, byte lightPopulated) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				level.putLong("LightPopulated", lightPopulated);
			}
		}
	}

	@MCVersionImplementation(100)
	public static class Structures implements ChunkFilter.Structures {

		@Override
		public CompoundTag getStructureReferences(ChunkData data) {
			CompoundTag structures = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Structures");
			return Helper.tagFromCompound(structures, "References");
		}

		@Override
		public CompoundTag getStructureStarts(ChunkData data) {
			CompoundTag structures = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Structures");
			return Helper.tagFromCompound(structures, "Starts");
		}
	}

	@MCVersionImplementation(100)
	public static class Blending implements ChunkFilter.Blending {

		@Override
		public void forceBlending(ChunkData data) {
			// do nothing
		}
	}

	@MCVersionImplementation(100)
	public static class Relocate implements ChunkFilter.Relocate {

		public boolean relocate(CompoundTag root, Point3i offset) {
			CompoundTag level = Helper.tagFromCompound(root, "Level");
			if (level == null) {
				return false;
			}

			// adjust or set chunk position
			level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
			level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());

			// adjust entity positions
			ListTag entities = Helper.tagFromCompound(level, "Entities");
			if (entities != null) {
				entities.forEach(v -> catchAndLog(() -> applyOffsetToEntity((CompoundTag) v, offset)));
			}

			// adjust tile entity positions
			ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities");
			if (tileEntities != null) {
				tileEntities.forEach(v -> catchAndLog(() -> applyOffsetToTileEntity((CompoundTag) v, offset)));
			}

			// adjust tile ticks
			ListTag tileTicks = Helper.tagFromCompound(level, "TileTicks");
			if (tileTicks != null) {
				tileTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// adjust liquid ticks
			ListTag liquidTicks = Helper.tagFromCompound(level, "LiquidTicks");
			if (liquidTicks != null) {
				liquidTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// Lights
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "Lights", offset.blockToSection()));

			// LiquidsToBeTicked
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "LiquidsToBeTicked", offset.blockToSection()));

			// ToBeTicked
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "ToBeTicked", offset.blockToSection()));

			// PostProcessing
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "PostProcessing", offset.blockToSection()));

			// adjust sections vertically
			ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
			if (sections != null) {
				ListTag newSections = new ListTag();
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					if (applyOffsetToSection(section, offset.blockToSection(), new Range(0, 15))) {
						newSections.add(section);
					}
				}
				level.put("Sections", newSections);
			}

			return true;
		}

		private void applyOffsetToEntity(CompoundTag entity, Point3i offset) {
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
				case "minecraft:shulker":
					Helper.applyIntOffsetIfRootPresent(entity, "APX", "APY", "APZ", offset);
					break;
				case "minecraft:vex":
					Helper.applyIntOffsetIfRootPresent(entity, "BoundX", "BoundY", "BoundZ", offset);
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
					if (entity.containsKey("Brain")) {
						CompoundTag brain = Helper.tagFromCompound(entity, "Brain");
						if (brain != null && brain.containsKey("memories")) {
							CompoundTag memories = Helper.tagFromCompound(brain, "memories");
							if (memories != null && !memories.isEmpty()) {
								CompoundTag meetingPoint = Helper.tagFromCompound(memories, "minecraft:meeting_point");
								if (meetingPoint != null && meetingPoint.containsKey("pos")) {
									if (meetingPoint.get("pos") instanceof IntArrayTag) {
										IntArrayTag pos = Helper.tagFromCompound(meetingPoint, "pos");
										Helper.applyOffsetToIntArrayPos(pos, offset);
									} else if (meetingPoint.get("pos") instanceof ListTag) {
										ListTag pos = Helper.tagFromCompound(meetingPoint, "pos");
										Helper.applyOffsetToIntListPos(pos, offset);
									}
								}
								CompoundTag home = Helper.tagFromCompound(memories, "minecraft:home");
								if (home != null && home.containsKey("pos")) {
									if (home.get("pos") instanceof IntArrayTag) {
										IntArrayTag pos = Helper.tagFromCompound(home, "pos");
										Helper.applyOffsetToIntArrayPos(pos, offset);
									} else if (home.get("pos") instanceof ListTag) {
										ListTag pos = Helper.tagFromCompound(home, "pos");
										Helper.applyOffsetToIntListPos(pos, offset);
									}
								}
								CompoundTag jobSite = Helper.tagFromCompound(memories, "minecraft:job_site");
								if (jobSite != null && jobSite.containsKey("pos")) {
									if (jobSite.get("pos") instanceof IntArrayTag) {
										IntArrayTag pos = Helper.tagFromCompound(jobSite, "pos");
										Helper.applyOffsetToIntArrayPos(pos, offset);
									} else if (jobSite.get("pos") instanceof ListTag) {
										ListTag pos = Helper.tagFromCompound(jobSite, "pos");
										Helper.applyOffsetToIntListPos(pos, offset);
									}
								}
							}
						}
					}
					break;
				case "minecraft:witch":
				case "minecraft:vindicator":
				case "minecraft:illusioner":
				case "minecraft:evoker":
					CompoundTag patrolTarget = Helper.tagFromCompound(entity, "PatrolTarget");
					Helper.applyIntOffsetIfRootPresent(patrolTarget, "X", "Y", "Z", offset);
					break;
				case "minecraft:falling_block":
					CompoundTag tileEntityData = Helper.tagFromCompound(entity, "TileEntityData");
					applyOffsetToTileEntity(tileEntityData, offset);
					break;
			}

			// recursively update passengers
			ListTag passengers = Helper.tagFromCompound(entity, "Passengers");
			if (passengers != null) {
				passengers.forEach(p -> applyOffsetToEntity((CompoundTag) p, offset));
			}

			Helper.fixEntityUUID(entity);
		}

		private void applyOffsetToTick(CompoundTag tick, Point3i offset) {
			Helper.applyIntOffsetIfRootPresent(tick, "x", "y", "z", offset);
		}

		private void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
			if (tileEntity == null) {
				return;
			}

			Helper.applyIntOffsetIfRootPresent(tileEntity, "x", "y", "z", offset);

			String id = Helper.stringFromCompound(tileEntity, "id", "");
			switch (id) {
				case "minecraft:end_gateway" -> {
					CompoundTag exitPortal = Helper.tagFromCompound(tileEntity, "ExitPortal");
					Helper.applyIntOffsetIfRootPresent(exitPortal, "X", "Y", "Z", offset);
				}
				case "minecraft:structure_block" -> Helper.applyIntOffsetIfRootPresent(tileEntity, "posX", "posY", "posZ", offset);
				case "minecraft:mob_spawner" -> {
					ListTag spawnPotentials = Helper.tagFromCompound(tileEntity, "SpawnPotentials");
					if (spawnPotentials != null) {
						for (CompoundTag spawnPotential : spawnPotentials.iterateType(CompoundTag.class)) {
							CompoundTag entity = Helper.tagFromCompound(spawnPotential, "Entity");
							applyOffsetToEntity(entity, offset);
						}
					}
				}
			}
		}
	}

	@MCVersionImplementation(100)
	public static class Merge implements ChunkFilter.Merge {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Sections", c -> ((CompoundTag) c).getInt("Y"));
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Entities", c -> ((CompoundTag) c).getList("Pos").getInt(1) >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileEntities", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "LiquidTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeListTagLists(source, destination, ranges, yOffset, "Lights");
			mergeListTagLists(source, destination, ranges, yOffset, "LiquidsToBeTicked");
			mergeListTagLists(source, destination, ranges, yOffset, "ToBeTicked");
			mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");

			// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
			fixEntityUUIDs(Helper.levelFromRoot(destination));
		}

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			CompoundTag root = new CompoundTag();
			CompoundTag level = new CompoundTag();
			level.putInt("xPos", absoluteLocation.getX());
			level.putInt("zPos", absoluteLocation.getZ());
			level.putString("Status", "postprocessed");
			root.put("Level", level);
			root.putInt("DataVersion", dataVersion);
			return root;
		}
	}

	@MCVersionImplementation(100)
	public static class Entities implements ChunkFilter.Entities {

		@Override
		public void deleteEntities(ChunkData data, List<Range> ranges) {
			ListTag entities = Helper.tagFromLevelFromRoot(data.region().getData(), "Entities", null);
			if (ranges == null) {
				if (entities != null) {
					entities.clear();
				}
			} else {
				for (int i = 0; i < entities.size(); i++) {
					CompoundTag entity = entities.getCompound(i);
					for (Range range : ranges) {
						ListTag entityPos = Helper.tagFromCompound(entity, "Pos");
						if (entityPos != null && entityPos.size() == 3) {
							if (range.contains(entityPos.getInt(1) >> 4)) {
								entities.remove(i);
								i--;
							}
						}
					}
				}
			}
		}

		@Override
		public ListTag getEntities(ChunkData data) {
			return Helper.tagFromLevelFromRoot(data.region().getData(), "Entities", null);
		}
	}

	@MCVersionImplementation(100)
	public static class MergeEntities implements ChunkFilter.MergeEntities {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			// nothing to do until 1.17
		}

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			// nothing to do until 1.17
			return null;
		}
	}

	@MCVersionImplementation(100)
	public static class MergePOI implements ChunkFilter.MergePOI {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			// nothing to do until 1.14
		}

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			// nothing to do until 1.14
			return null;
		}
	}

	@MCVersionImplementation(100)
	public static class RelocateEntities implements ChunkFilter.RelocateEntities {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			// nothing to do until 1.17
			return true;
		}
	}

	@MCVersionImplementation(100)
	public static class RelocatePOI implements ChunkFilter.RelocatePOI {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			// poi was introduced in 1.14, so we do nothing here
			return true;
		}
	}

	@MCVersionImplementation(100)
	public static class Heightmap implements ChunkFilter.Heightmap {

		private static final Gson GSON = new GsonBuilder()
				.setPrettyPrinting()
				.create();

		private static final Set<Short> nonWorldSurfaceBlocks = FileHelper.loadFromResource(
				"mapping/java_1_9/heightmaps_legacy.json",
				r -> GSON.fromJson(r, new TypeToken<>() {}));

		@Override
		public void worldSurface(ChunkData data) {
			setHeightMap(Helper.getRegion(data), getHeightMap(Helper.getRegion(data), block -> !nonWorldSurfaceBlocks.contains(block)));
		}

		@Override
		public void oceanFloor(ChunkData data) {
			// nothing to do until 1.13
		}

		@Override
		public void motionBlocking(ChunkData data) {
			// nothing to do until 1.13
		}

		@Override
		public void motionBlockingNoLeaves(ChunkData data) {
			// nothing to do until 1.13
		}

		protected void setHeightMap(CompoundTag data, int[] heightmap) {
			if (data == null) {
				return;
			}
			CompoundTag level = data.getCompoundTag("Level");
			if (level == null) {
				return;
			}
			level.putIntArray("HeightMap", heightmap);
		}

		protected int[] getHeightMap(CompoundTag data, Predicate<Short> matcher) {
			ListTag sections = Helper.getSectionsFromLevelFromRoot(data, "Sections");
			if (sections == null) {
				return new int[256];
			}

			byte[][] blocksArray = new byte[16][];
			for (CompoundTag s : sections.iterateType(CompoundTag.class)) {
				if (!s.containsKey("Blocks")) {
					continue;
				}
				int y = Helper.numberFromCompound(s, "Y", -1).intValue();
				byte[] b = Helper.byteArrayFromCompound(s, "Blocks");
				if (y >= 0 && y < 16 && b != null) {
					blocksArray[y] = b;
				}
			}

			int[] heightmap = new int[256];

			// loop over x/z
			for (int cx = 0; cx < 16; cx++) {
				loop:
				for (int cz = 0; cz < 16; cz++) {
					for (int i = 15; i >= 0; i--) {
						byte[] blocks = blocksArray[i];
						if (blocks == null) {
							continue;
						}
						for (int cy = 15; cy >= 0; cy--) {
							int index = cy * 256 + cz * 16 + cx;
							short block = (short) (blocks[index] & 0xFF);
							if (matcher.test(block)) {
								heightmap[cz * 16 + cx] = i * 16 + cy + 1;
								continue loop;
							}
						}
					}
				}
			}
			return heightmap;
		}
	}
}
