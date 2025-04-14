package net.querz.mcaselector.version.java_1_15;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_13.ChunkFilter_18w06a;
import net.querz.mcaselector.version.java_1_14.ChunkFilter_19w02a;
import net.querz.mcaselector.version.java_1_14.ChunkFilter_19w11a;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.DoubleTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.ListTag;
import java.util.Arrays;
import java.util.List;
import static net.querz.mcaselector.util.validation.ValidationHelper.*;

public class ChunkFilter_19w36a {

	@MCVersionImplementation(2203)
	public static class Biomes extends ChunkFilter_18w06a.Biomes {

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				int[] biomes = new int[1024];
				Arrays.fill(biomes, (byte) biome.getID());
				level.putIntArray("Biomes", biomes);
			}
		}
	}

	@MCVersionImplementation(2203)
	public static class Merge extends ChunkFilter_19w02a.Merge {

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
			mergeStructures(source, destination, ranges, yOffset);

			// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
			fixEntityUUIDs(Helper.levelFromRoot(destination));

			mergeBiomes(source, destination, ranges, yOffset);
		}

		protected void mergeBiomes(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			IntArrayTag sourceBiomes = Helper.tagFromLevelFromRoot(source, "Biomes");
			IntArrayTag destinationBiomes = Helper.tagFromLevelFromRoot(destination, "Biomes");

			if (destinationBiomes == null) {
				// if there is no destination, we will let minecraft set the biome
				destinationBiomes = new IntArrayTag(new int[1024]);
				Arrays.fill(destinationBiomes.getValue(), -1);
			}

			if (sourceBiomes == null) {
				// if there is no source biome, we set the biome to -1
				// merge biomes
				for (Range range : ranges) {
					int m = Math.min(range.getTo() + yOffset, 15);
					for (int i = Math.max(range.getFrom() + yOffset, 0); i <= m; i++) {
						setSectionBiomes(-1, destinationBiomes.getValue(), i);
					}
				}
			} else {
				for (Range range : ranges) {
					int m = Math.min(range.getTo() - yOffset, 15);
					for (int i = Math.max(range.getFrom() - yOffset, 0); i <= m; i++) {
						copySectionBiomes(sourceBiomes.getValue(), destinationBiomes.getValue(), i);
					}
				}
			}
		}

		protected void copySectionBiomes(int[] sourceBiomes, int[] destinationBiomes, int sectionY) {
			for (int y = 0; y < 4; y++) {
				int biomeY = sectionY * 4 + y;
				for (int x = 0; x < 4; x++) {
					for (int z = 0; z < 4; z++) {
						setBiomeAt(destinationBiomes, x, biomeY, z, getBiomeAt(sourceBiomes, x, biomeY, z));
					}
				}
			}
		}

		protected void setSectionBiomes(int biome, int[] destinationBiomes, int sectionY) {
			for (int y = 0; y < 4; y++) {
				int biomeY = sectionY * 4 + y;
				for (int x = 0; x < 4; x++) {
					for (int z = 0; z < 4; z++) {
						setBiomeAt(destinationBiomes, x, biomeY, z, biome);
					}
				}
			}
		}

		protected int getBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ) {
			if (biomes == null || biomes.length != 1024) {
				return -1;
			}
			return biomes[getBiomeIndex(biomeX, biomeY, biomeZ)];
		}

		protected void setBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ, int biomeID) {
			if (biomes == null || biomes.length != 1024) {
				biomes = new int[1024];
				Arrays.fill(biomes, -1);
			}
			biomes[getBiomeIndex(biomeX, biomeY, biomeZ)] = biomeID;
		}

		protected int getBiomeIndex(int x, int y, int z) {
			return y * 16 + z * 4 + x;
		}
	}

	@MCVersionImplementation(2203)
	public static class Relocate extends ChunkFilter_19w11a.Relocate {

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

			// adjust liquid ticks
			ListTag liquidTicks = Helper.tagFromCompound(level, "LiquidTicks");
			if (liquidTicks != null) {
				liquidTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// adjust structures
			CompoundTag structures = Helper.tagFromCompound(level, "Structures");
			if (structures != null) {
				catchAndLog(() -> applyOffsetToStructures(structures, offset));
			}

			// Biomes
			catchAndLog(() -> applyOffsetToBiomes(Helper.tagFromCompound(level, "Biomes"), offset.blockToSection(), 16));

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

		protected void applyOffsetToBiomes(IntArrayTag biomes, Point3i offset, int numSections) {
			if (biomes == null || biomes.getValue() == null || biomes.getValue().length != 1024) {
				return;
			}

			int[] biomesArray = biomes.getValue();
			int[] newBiomes = new int[1024];

			for (int x = 0; x < 4; x++) {
				for (int z = 0; z < 4; z++) {
					for (int y = 0; y < 64; y++) {
						if (y + offset.getY() * 4 < 0 || y + offset.getY() * 4 > 63) {
							break;
						}
						int biome = biomesArray[y * 16 + z * 4 + x];
						newBiomes[(y + offset.getY() * 4) * 16 + z * 4 + x] = biome;
					}
				}
			}

			biomes.setValue(newBiomes);
		}

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
					applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:meeting_point"), offset);
					applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:home"), offset);
					applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:job_site"), offset);
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
				applyOffsetToTileEntity(tileEntityData, offset);
			}

			// recursively update passengers
			ListTag passengers = Helper.tagFromCompound(entity, "Passengers");
			if (passengers != null) {
				passengers.forEach(p -> applyOffsetToEntity((CompoundTag) p, offset));
			}

			Helper.fixEntityUUID(entity);
		}

		@Override
		protected void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
			if (tileEntity == null) {
				return;
			}

			Helper.applyIntOffsetIfRootPresent(tileEntity, "x", "y", "z", offset);

			String id = Helper.stringFromCompound(tileEntity, "id", "");
			switch (id) {
			case "minecraft:bee_nest":
			case "minecraft:beehive":
				CompoundTag flowerPos = Helper.tagFromCompound(tileEntity, "FlowerPos");
				Helper.applyIntOffsetIfRootPresent(flowerPos, "X", "Y", "Z", offset);
				ListTag bees = Helper.tagFromCompound(tileEntity, "Bees");
				if (bees != null) {
					for (CompoundTag bee : bees.iterateType(CompoundTag.class)) {
						applyOffsetToEntity(Helper.tagFromCompound(bee, "EntityData"), offset);
					}
				}
				break;
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
}
