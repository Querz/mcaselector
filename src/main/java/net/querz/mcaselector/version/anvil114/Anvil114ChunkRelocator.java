package net.querz.mcaselector.version.anvil114;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.*;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.silent;

public class Anvil114ChunkRelocator implements ChunkRelocator {

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
			entities.forEach(v -> applyOffsetToEntity((CompoundTag) v, offset));
		}

		// adjust tile entity positions
		ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities");
		if (tileEntities != null) {
			tileEntities.forEach(v -> applyOffsetToTileEntity((CompoundTag) v, offset));
		}

		// adjust tile ticks
		ListTag tileTicks = Helper.tagFromCompound(level, "TileTicks");
		if (tileTicks != null) {
			tileTicks.forEach(v -> applyOffsetToTick((CompoundTag) v, offset));
		}

		// adjust liquid ticks
		ListTag liquidTicks = Helper.tagFromCompound(level, "LiquidTicks");
		if (liquidTicks != null) {
			liquidTicks.forEach(v -> applyOffsetToTick((CompoundTag) v, offset));
		}

		// adjust structures
		CompoundTag structures = Helper.tagFromCompound(level, "Structures");
		if (structures != null) {
			applyOffsetToStructures(structures, offset);
		}

		// Lights
		Helper.applyOffsetToListOfShortTagLists(level, "Lights", offset.blockToSection());

		// LiquidsToBeTicked
		Helper.applyOffsetToListOfShortTagLists(level, "LiquidsToBeTicked", offset.blockToSection());

		// ToBeTicked
		Helper.applyOffsetToListOfShortTagLists(level, "ToBeTicked", offset.blockToSection());

		// PostProcessing
		Helper.applyOffsetToListOfShortTagLists(level, "PostProcessing", offset.blockToSection());

		// adjust sections vertically
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections != null) {
			ListTag newSections = new ListTag();
			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				if (applyOffsetToSection(section, offset.blockToSection(), 0, 15)) {
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
		Helper.applyIntOffsetIfRootPresent(entity, "xTile", "yTile", "zTile", offset);

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
		case "minecraft:villager":
			CompoundTag memories = Helper.tagFromCompound(Helper.tagFromCompound(entity, "Brain"), "memories");
			if (memories != null && memories.size() > 0) {
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

	private void applyOffsetToVillagerMemory(CompoundTag memory, Point3i offset) {
		IntArrayTag mPos = Helper.tagFromCompound(memory, "pos");
		Helper.applyOffsetToIntArrayPos(mPos, offset);
		if (mPos == null) {
			ListTag lPos = Helper.tagFromCompound(memory, "pos");
			Helper.applyOffsetToIntListPos(lPos, offset);
		}
	}

	private void applyOffsetToStructures(CompoundTag structures, Point3i offset) { // 1.13
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
