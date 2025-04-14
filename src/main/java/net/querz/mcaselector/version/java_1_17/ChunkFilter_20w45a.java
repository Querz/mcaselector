package net.querz.mcaselector.version.java_1_17;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_15.ChunkFilter_19w36a;
import net.querz.mcaselector.version.java_1_16.ChunkFilter_20w13a;
import net.querz.mcaselector.version.java_1_16.ChunkFilter_20w17a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;
import net.querz.nbt.*;
import java.util.List;
import static net.querz.mcaselector.util.validation.ValidationHelper.*;

public class ChunkFilter_20w45a {

	@MCVersionImplementation(2681)
	public static class Entities implements ChunkFilter.Entities {

		@Override
		public void deleteEntities(ChunkData data, List<Range> ranges) {
			ListTag entities = Helper.tagFromLevelFromRoot(Helper.getEntities(data), "Entities");
			deleteEntities(entities, ranges);

			// delete proto-entities
			ListTag protoEntities = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Entities");
			deleteEntities(protoEntities, ranges);
		}

		protected void deleteEntities(ListTag entities, List<Range> ranges) {
			if (entities == null) {
				return;
			}
			if (ranges == null) {
				entities.clear();
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
			return Helper.tagFromCompound(Helper.getEntities(data), "Entities");
		}
	}

	@MCVersionImplementation(2681)
	public static class MergeEntities implements ChunkFilter.MergeEntities {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			mergeCompoundTagLists(source, destination, ranges, yOffset, "Entities", c -> ((CompoundTag) c).getList("Pos").getInt(1) >> 4);
		}

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			CompoundTag root = new CompoundTag();
			int[] position = new int[]{
					absoluteLocation.getX(),
					absoluteLocation.getZ()
			};
			root.putIntArray("Position", position);
			root.putInt("DataVersion", dataVersion);
			root.put("Entities", new ListTag());
			return root;
		}
	}

	@MCVersionImplementation(2681)
	public static class RelocateEntities implements ChunkFilter.RelocateEntities {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			int[] position = Helper.intArrayFromCompound(root, "Position");
			if (position == null || position.length != 2) {
				return false;
			}

			position[0] += offset.blockToChunk().getX();
			position[1] += offset.blockToChunk().getZ();

			// adjust entity positions
			ListTag entities = Helper.tagFromCompound(root, "Entities");
			if (entities != null) {
				entities.forEach(v -> catchAndLog(() -> applyOffsetToEntity((CompoundTag) v, offset)));
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

	@MCVersionImplementation(2681)
	public static class Relocate extends ChunkFilter_20w13a.Relocate {

		static Relocate instance;

		public Relocate() {
			// will be instantiated by the VersionHandler
			instance = this;
		}

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			CompoundTag level = Helper.tagFromCompound(root, "Level");
			if (level == null) {
				return false;
			}

			// adjust or set chunk position
			level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
			level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());

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

		// make helper methods visible in this package, so they can be reused by RelocateEntities

		@Override
		protected void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
			super.applyOffsetToTileEntity(tileEntity, offset);
		}

		@Override
		protected void applyOffsetToVillagerMemory(CompoundTag memory, Point3i offset) {
			super.applyOffsetToVillagerMemory(memory, offset);
		}

		@Override
		protected void applyOffsetToItem(CompoundTag item, Point3i offset) {
			super.applyOffsetToItem(item, offset);
		}
	}

	@MCVersionImplementation(2681)
	public static class Merge extends ChunkFilter_19w36a.Merge {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Sections", c -> ((CompoundTag) c).getInt("Y"));
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileEntities", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "LiquidTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeListTagLists(source, destination, ranges, yOffset, "Lights");
			mergeListTagLists(source, destination, ranges, yOffset, "LiquidsToBeTicked");
			mergeListTagLists(source, destination, ranges, yOffset, "ToBeTicked");
			mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");
			mergeStructures(source, destination, ranges, yOffset);
		}
	}

	@MCVersionImplementation(2681)
	public static class Heightmap extends ChunkFilter_20w17a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_17/heightmaps_20w45a.json", HeightmapConfig::load);
		}
	}
}
