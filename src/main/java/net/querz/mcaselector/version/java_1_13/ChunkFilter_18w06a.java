package net.querz.mcaselector.version.java_1_13;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.math.Bits;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.*;
import java.util.*;
import java.util.function.Predicate;
import static net.querz.mcaselector.util.validation.ValidationHelper.*;

public class ChunkFilter_18w06a {

	@MCVersionImplementation(1466)
	public static class Biomes implements ChunkFilter.Biomes {

		@Override
		public boolean matchBiomes(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			IntArrayTag biomesTag = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Biomes");
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
		public boolean matchAnyBiome(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			IntArrayTag biomesTag = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Biomes");
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
		public void changeBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			IntArrayTag biomesTag = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Biomes");
			if (biomesTag != null) {
				Arrays.fill(biomesTag.getValue(), biome.getID());
			}
		}

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				int[] biomes = new int[256];
				Arrays.fill(biomes, (byte) biome.getID());
				level.putIntArray("Biomes", biomes);
			}
		}
	}

	@MCVersionImplementation(1466)
	public static class LightPopulated implements ChunkFilter.LightPopulated {

		@Override
		public ByteTag getLightPopulated(ChunkData data) {
			// removed in 18w06a
			return null;
		}

		@Override
		public void setLightPopulated(ChunkData data, byte lightPopulated) {
			// removed in 18w06a
		}
	}

	@MCVersionImplementation(1466)
	public static class Status implements ChunkFilter.Status {

		@Override
		public StringTag getStatus(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Status");
		}

		@Override
		public void setStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				level.putString("Status", status.getStatus());
			}
		}

		@Override
		public boolean matchStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			StringTag tag = getStatus(data);
			if (tag == null) {
				return false;
			}
			return status.getStatus().equals(tag.getValue());
		}
	}

	@MCVersionImplementation(1466)
	public static class Merge implements ChunkFilter.Merge {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Sections", c -> ((CompoundTag) c).getInt("Y"));
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Entities", c -> ((CompoundTag) c).getList("Pos").getInt(1) >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileEntities", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeListTagLists(source, destination, ranges, yOffset, "Lights");
			mergeListTagLists(source, destination, ranges, yOffset, "ToBeTicked");
			mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");
			mergeStructures(source, destination, ranges, yOffset);

			// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
			fixEntityUUIDs(Helper.levelFromRoot(destination));
		}

		protected void mergeStructures(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			CompoundTag sourceStarts = Helper.tagFromCompound(Helper.tagFromLevelFromRoot(source, "Structures", new CompoundTag()), "Starts", new CompoundTag());
			CompoundTag destinationStarts = Helper.tagFromCompound(Helper.tagFromLevelFromRoot(destination, "Structures", new CompoundTag()), "Starts", new CompoundTag());

			if (!destinationStarts.isEmpty()) {
				// remove BBs from destination
				for (Map.Entry<String, Tag> start : destinationStarts) {
					ListTag children = Helper.tagFromCompound(start.getValue(), "Children", null);
					if (children != null) {
						child: for (int i = 0; i < children.size(); i++) {
							CompoundTag child = children.getCompound(i);
							int[] bb = Helper.intArrayFromCompound(child, "BB");
							if (bb != null && bb.length == 6) {
								for (Range range : ranges) {
									if (range.contains(bb[1] >> 4) && range.contains(bb[4] >> 4)) {
										children.remove(i);
										i--;
										continue child;
									}
								}
							}
						}
					}

					// if we removed all children, we check the start BB
					if (children == null || children.isEmpty()) {
						int[] bb = Helper.intArrayFromCompound(start.getValue(), "BB");
						if (bb != null && bb.length == 6) {
							for (Range range : ranges) {
								if (range.contains(bb[1] >> 4) && range.contains(bb[4] >> 4)) {
									CompoundTag emptyStart = new CompoundTag();
									emptyStart.putString("id", "INVALID");
									destinationStarts.put(start.getKey(), emptyStart);
									break;
								}
							}
						}
					}
				}
			}

			// add BBs from source to destination
			// if child BB doesn't exist in destination, we copy start over to destination
			for (Map.Entry<String, Tag> start : sourceStarts) {
				ListTag children = Helper.tagFromCompound(start.getValue(), "Children", null);
				if (children != null) {
					child:
					for (int i = 0; i < children.size(); i++) {
						CompoundTag child = children.getCompound(i);
						int[] bb = Helper.intArrayFromCompound(child, "BB");
						if (bb == null) {
							continue;
						}
						for (Range range : ranges) {
							if (range.contains(bb[1] >> 4 - yOffset) || range.contains(bb[4] >> 4 - yOffset)) {
								CompoundTag destinationStart = Helper.tagFromCompound(destinationStarts, start.getKey(), null);
								if (destinationStart == null || "INVALID".equals(destinationStart.getString("id"))) {
									destinationStart = ((CompoundTag) start.getValue()).copy();

									// we need to remove the children, we don't want all of them
									ListTag clonedDestinationChildren = Helper.tagFromCompound(destinationStart, "Children", null);
									if (clonedDestinationChildren != null) {
										clonedDestinationChildren.clear();
									}
									destinationStarts.put(start.getKey(), destinationStart);
								}

								ListTag destinationChildren = Helper.tagFromCompound(destinationStarts.get(start.getKey()), "Children", null);
								if (destinationChildren == null) {
									destinationChildren = new ListTag();
									destinationStart.put("Children", destinationChildren);
								}

								destinationChildren.add(children.get(i));
								continue child;
							}
						}
					}
				}
			}
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

	@MCVersionImplementation(1466)
	public static class Relocate implements ChunkFilter.Relocate {

		@Override
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

			// adjust structures
			CompoundTag structures = Helper.tagFromCompound(level, "Structures");
			if (structures != null) {
				catchAndLog(() -> applyOffsetToStructures(structures, offset));
			}

			// Lights
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "Lights", offset.blockToSection()));

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
			}

			// recursively update passengers
			ListTag passengers = Helper.tagFromCompound(entity, "Passengers");
			if (passengers != null) {
				passengers.forEach(p -> applyOffsetToEntity((CompoundTag) p, offset));
			}

			Helper.fixEntityUUID(entity);
		}

		protected void applyOffsetToStructures(CompoundTag structures, Point3i offset) { // 1.13
			Point3i chunkOffset = offset.blockToChunk();

			// update references
			CompoundTag references = Helper.tagFromCompound(structures, "References");
			if (references != null) {
				for (Map.Entry<String, Tag> entry : references) {
					long[] reference = silent(() -> ((LongArrayTag) entry.getValue()).getValue(), null);
					if (reference != null) {
						for (int i = 0; i < reference.length; i++) {
							int x = (int) (reference[i]);
							int z = (int) (reference[i] >> 32);
							reference[i] = ((long) (z + chunkOffset.getZ()) & 0xFFFFFFFFL) << 32 | (long) (x + chunkOffset.getX()) & 0xFFFFFFFFL;
						}
					}
				}
			}

			// update starts
			CompoundTag starts = Helper.tagFromCompound(structures, "Starts");
			if (starts != null) {
				for (Map.Entry<String, Tag> entry : starts) {
					CompoundTag structure = silent(() -> (CompoundTag) entry.getValue(), null);
					if ("INVALID".equals(Helper.stringFromCompound(structure, "id"))) {
						continue;
					}
					Helper.applyIntIfPresent(structure, "ChunkX", chunkOffset.getX());
					Helper.applyIntIfPresent(structure, "ChunkZ", chunkOffset.getZ());
					Helper.applyOffsetToBB(Helper.intArrayFromCompound(structure, "BB"), offset);

					ListTag processed = Helper.tagFromCompound(structure, "Processed");
					if (processed != null) {
						for (CompoundTag chunk : processed.iterateType(CompoundTag.class)) {
							Helper.applyIntIfPresent(chunk, "X", chunkOffset.getX());
							Helper.applyIntIfPresent(chunk, "Z", chunkOffset.getZ());
						}
					}

					ListTag children = Helper.tagFromCompound(structure, "Children");
					if (children != null) {
						for (CompoundTag child : children.iterateType(CompoundTag.class)) {
							Helper.applyIntOffsetIfRootPresent(child, "TPX", "TPY", "TPZ", offset);
							Helper.applyIntOffsetIfRootPresent(child, "PosX", "PosY", "PosZ", offset);
							Helper.applyOffsetToBB(Helper.intArrayFromCompound(child, "BB"), offset);

							ListTag entrances = Helper.tagFromCompound(child, "Entrances");
							if (entrances != null) {
								entrances.forEach(e -> Helper.applyOffsetToBB(((IntArrayTag) e).getValue(), offset));
							}

							ListTag junctions = Helper.tagFromCompound(child, "junctions");
							if (junctions != null) {
								for (CompoundTag junction : junctions.iterateType(CompoundTag.class)) {
									Helper.applyIntOffsetIfRootPresent(junction, "source_x", "source_y", "source_z", offset);
								}
							}
						}
					}
				}
			}
		}

		protected void applyOffsetToTick(CompoundTag tick, Point3i offset) {
			Helper.applyIntOffsetIfRootPresent(tick, "x", "y", "z", offset);
		}

		protected void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
			if (tileEntity == null) {
				return;
			}

			Helper.applyIntOffsetIfRootPresent(tileEntity, "x", "y", "z", offset);

			String id = Helper.stringFromCompound(tileEntity, "id", "");
			switch (id) {
				case "minecraft:end_gateway":
					CompoundTag exitPortal = Helper.tagFromCompound(tileEntity, "ExitPortal");
					Helper.applyIntOffsetIfRootPresent(exitPortal, "X", "Y", "Z", offset);
					break;
				case "minecraft:structure_block":
					Helper.applyIntOffsetIfRootPresent(tileEntity, "posX", "posY", "posZ", offset);
					break;
				case "minecraft:mob_spawner":
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

	@MCVersionImplementation(1466)
	public static class Heightmap implements ChunkFilter.Heightmap {

		private static final Gson GSON = new GsonBuilder()
				.setPrettyPrinting()
				.create();

		// only "RAIN" and "LIGHT" exist in fully generated chunks, "LIQUID" and "SOLID" seem to only exist in proto chunks
		private record HeightmapData(
				@SerializedName("rain") Set<String> rain,
				@SerializedName("light") Set<String> light) {}

		private HeightmapData heightmaps;

		public Heightmap() {
			loadCfg();
		}

		protected void loadCfg() {
			heightmaps = FileHelper.loadFromResource(
					"mapping/java_1_13/heightmaps_18w06a.json",
					r -> GSON.fromJson(r, new TypeToken<>() {}));
		}

		@Override
		public void worldSurface(ChunkData data) {
			setHeightMap(Helper.getRegion(data), "LIGHT", getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && heightmaps.light.contains(name);
			}));
		}

		@Override
		public void oceanFloor(ChunkData data) {}

		@Override
		public void motionBlocking(ChunkData data) {
			setHeightMap(Helper.getRegion(data), "RAIN", getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && heightmaps.rain.contains(name);
			}));
		}

		@Override
		public void motionBlockingNoLeaves(ChunkData data) {}

		protected void setHeightMap(CompoundTag root, String name, long[] heightmap) {
			if (root == null) {
				return;
			}
			CompoundTag level = Helper.levelFromRoot(root);
			if (level == null) {
				return;
			}
			CompoundTag heightmapTag = (CompoundTag) level.computeIfAbsent("Heightmaps", k -> new CompoundTag());
			heightmapTag.putLongArray(name, heightmap);
		}

		protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
			ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
			if (sections == null) {
				return new long[36];
			}

			ListTag[] palettes = new ListTag[16];
			long[][] blockStatesArray = new long[16][];
			sections.forEach(s -> {
				ListTag p = Helper.tagFromCompound(s, "Palette");
				long[] b = Helper.longArrayFromCompound(s, "BlockStates");
				int y = Helper.numberFromCompound(s, "Y", -1).intValue();
				if (y >= 0 && y <= 15 && p != null && b != null) {
					palettes[y] = p;
					blockStatesArray[y] = b;
				}
			});

			short[] heightmap = new short[256];

			// loop over x/z
			for (int cx = 0; cx < 16; cx++) {
				loop:
				for (int cz = 0; cz < 16; cz++) {
					for (int i = 15; i >= 0; i--) {
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
			return applyHeightMap(heightmap);
		}

		protected long[] applyHeightMap(short[] rawHeightmap) {
			long[] data = new long[36];
			int offset = 0;
			int index = 0;
			for (int i = 0; i < 36; i++) {
				long l = 0L;
				for (int j = 0; j < 8 && index < 256; j++, index++) {
					int shift = 9 * j - offset;
					if (shift < 0) {
						l += ((long) rawHeightmap[index] >> -shift);
					} else {
						l += ((long) rawHeightmap[index] << shift);
					}
				}
				offset++;
				if (offset == 9) {
					offset = 0;
				} else {
					index--;
				}
				data[i] = l;
			}
			return data;
		}

		protected CompoundTag getBlockAt(int index, long[] blockStates, ListTag palette) {
			return palette.getCompound(getPaletteIndex(index, blockStates));
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
	}
}
