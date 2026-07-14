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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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

		protected Map<String, BiConsumer<CompoundTag, Point3i>> functions = new HashMap<>();

		public RelocateEntities() {
			functions.put("minecraft:dolphin", (e, o) -> Helper.applyIntOffsetIfRootPresent(e, "TreasurePosX", "TreasurePosY", "TreasurePosZ", o));
			functions.put("minecraft:phantom", (e, o) -> Helper.applyIntOffsetIfRootPresent(e, "AX", "AY", "AZ", o));
			functions.put("minecraft:shulker", (e, o) -> Helper.applyIntOffsetIfRootPresent(e, "APX", "APY", "APZ", o));
			functions.put("minecraft:turtle", (e, o) -> {
				Helper.applyIntOffsetIfRootPresent(e, "HomePosX", "HomePosY", "HomePosZ", o);
				Helper.applyIntOffsetIfRootPresent(e, "TravelPosX", "TravelPosY", "TravelPosZ", o);
			});
			functions.put("minecraft:vex", (e, o) -> Helper.applyIntOffsetIfRootPresent(e, "BoundX", "BoundY", "BoundZ", o));
			functions.put("minecraft:wandering_trader", (e, o) -> {
				CompoundTag wanderTarget = Helper.tagFromCompound(e, "WanderTarget");
				Helper.applyIntOffsetIfRootPresent(wanderTarget, "X", "Y", "Z", o);
			});
			functions.put("minecraft:shulker_bullet", (e, o) -> {
				CompoundTag owner = Helper.tagFromCompound(e, "Owner");
				Helper.applyIntOffsetIfRootPresent(owner, "X", "Y", "Z", o);
				CompoundTag target = Helper.tagFromCompound(e, "Target");
				Helper.applyIntOffsetIfRootPresent(target, "X", "Y", "Z", o);
			});
			functions.put("minecraft:end_crystal", (e, o) -> {
				CompoundTag beamTarget = Helper.tagFromCompound(e, "BeamTarget");
				Helper.applyIntOffsetIfRootPresent(beamTarget, "X", "Y", "Z", o);
			});
			BiConsumer<CompoundTag, Point3i> framed = (e, o) -> Helper.applyIntOffsetIfRootPresent(e, "TileX", "TileY", "TileZ", o);
			functions.put("minecraft:item_frame", framed);
			functions.put("minecraft:painting", framed);
			functions.put("minecraft:villager", (e, o) -> {
				CompoundTag memories = Helper.tagFromCompound(Helper.tagFromCompound(e, "Brain"), "memories");
				if (memories != null && !memories.isEmpty()) {
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:meeting_point"), o);
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:home"), o);
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:job_site"), o);
				}
			});
			BiConsumer<CompoundTag, Point3i> pillagers = (e, o) -> {
				CompoundTag patrolTarget = Helper.tagFromCompound(e, "PatrolTarget");
				Helper.applyIntOffsetIfRootPresent(patrolTarget, "X", "Y", "Z", o);
			};
			functions.put("minecraft:pillager", pillagers);
			functions.put("minecraft:witch", pillagers);
			functions.put("minecraft:vindicator", pillagers);
			functions.put("minecraft:ravager", pillagers);
			functions.put("minecraft:illusioner", pillagers);
			functions.put("minecraft:evoker", pillagers);
			functions.put("minecraft:falling_block", (e, o) -> {
				CompoundTag tileEntityData = Helper.tagFromCompound(e, "TileEntityData");
				Relocate.instance.applyOffsetToTileEntity(tileEntityData, o);
			});
		}

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

			BiConsumer<CompoundTag, Point3i> entityFunction = functions.get(id);
			if (entityFunction != null) {
				entityFunction.accept(entity, offset);
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
