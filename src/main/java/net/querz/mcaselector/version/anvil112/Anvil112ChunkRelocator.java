package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.*;

public class Anvil112ChunkRelocator implements ChunkRelocator {

	public boolean relocate(CompoundTag root, Point3i offset) {
		CompoundTag level = NbtHelper.tagFromCompound(root, "Level");
		if (level == null) {
			return false;
		}

		// adjust or set chunk position
		level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
		level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());

		// adjust entity positions
		ListTag entities = NbtHelper.tagFromCompound(level, "Entities");
		if (entities != null) {
			entities.forEach(v -> applyOffsetToEntity((CompoundTag) v, offset));
		}

		// adjust tile entity positions
		ListTag tileEntities = NbtHelper.tagFromCompound(level, "TileEntities");
		if (tileEntities != null) {
			tileEntities.forEach(v -> applyOffsetToTileEntity((CompoundTag) v, offset));
		}

		// adjust tile ticks
		ListTag tileTicks = NbtHelper.tagFromCompound(level, "TileTicks");
		if (tileTicks != null) {
			tileTicks.forEach(v -> applyOffsetToTick((CompoundTag) v, offset));
		}

		// adjust liquid ticks
		ListTag liquidTicks = NbtHelper.tagFromCompound(level, "LiquidTicks");
		if (liquidTicks != null) {
			liquidTicks.forEach(v -> applyOffsetToTick((CompoundTag) v, offset));
		}

		// Lights
		NbtHelper.applyOffsetToListOfShortTagLists(level, "Lights", offset.blockToSection());

		// LiquidsToBeTicked
		NbtHelper.applyOffsetToListOfShortTagLists(level, "LiquidsToBeTicked", offset.blockToSection());

		// ToBeTicked
		NbtHelper.applyOffsetToListOfShortTagLists(level, "ToBeTicked", offset.blockToSection());

		// PostProcessing
		NbtHelper.applyOffsetToListOfShortTagLists(level, "PostProcessing", offset.blockToSection());

		// adjust sections vertically
		ListTag sections = NbtHelper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections != null) {
			ListTag newSections = new ListTag();
			for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
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

		ListTag entityPos = NbtHelper.tagFromCompound(entity, "Pos");
		if (entityPos != null && entityPos.size() == 3) {
			entityPos.set(0, DoubleTag.valueOf(entityPos.getDouble(0) + offset.getX()));
			entityPos.set(1, DoubleTag.valueOf(entityPos.getDouble(1) + offset.getY()));
			entityPos.set(2, DoubleTag.valueOf(entityPos.getDouble(2) + offset.getZ()));
		}

		// leashed entities
		CompoundTag leash = NbtHelper.tagFromCompound(entity, "Leash");
		NbtHelper.applyIntOffsetIfRootPresent(leash, "X", "Y", "Z", offset);

		// projectiles
		NbtHelper.applyIntOffsetIfRootPresent(entity, "xTile", "yTile", "zTile", offset);

		// entities that have a sleeping place
		NbtHelper.applyIntOffsetIfRootPresent(entity, "SleepingX", "SleepingY", "SleepingZ", offset);

		// positions for specific entity types
		String id = NbtHelper.stringFromCompound(entity, "id", "");
		switch (id) {
		case "minecraft:shulker":
			NbtHelper.applyIntOffsetIfRootPresent(entity, "APX", "APY", "APZ", offset);
			break;
		case "minecraft:vex":
			NbtHelper.applyIntOffsetIfRootPresent(entity, "BoundX", "BoundY", "BoundZ", offset);
			break;
		case "minecraft:shulker_bullet":
			CompoundTag owner = NbtHelper.tagFromCompound(entity, "Owner");
			NbtHelper.applyIntOffsetIfRootPresent(owner, "X", "Y", "Z", offset);
			CompoundTag target = NbtHelper.tagFromCompound(entity, "Target");
			NbtHelper.applyIntOffsetIfRootPresent(target, "X", "Y", "Z", offset);
			break;
		case "minecraft:end_crystal":
			CompoundTag beamTarget = NbtHelper.tagFromCompound(entity, "BeamTarget");
			NbtHelper.applyIntOffsetIfRootPresent(beamTarget, "X", "Y", "Z", offset);
			break;
		case "minecraft:item_frame":
		case "minecraft:painting":
			NbtHelper.applyIntOffsetIfRootPresent(entity, "TileX", "TileY", "TileZ", offset);
			break;
		case "minecraft:villager":
			if (entity.containsKey("Brain")) {
				CompoundTag brain = NbtHelper.tagFromCompound(entity, "Brain");
				if (brain != null && brain.containsKey("memories")) {
					CompoundTag memories = NbtHelper.tagFromCompound(brain, "memories");
					if (memories != null && memories.size() > 0) {
						CompoundTag meetingPoint = NbtHelper.tagFromCompound(memories, "minecraft:meeting_point");
						if (meetingPoint != null && meetingPoint.containsKey("pos")) {
							if (meetingPoint.get("pos") instanceof IntArrayTag) {
								IntArrayTag pos = NbtHelper.tagFromCompound(meetingPoint, "pos");
								NbtHelper.applyOffsetToIntArrayPos(pos, offset);
							} else if (meetingPoint.get("pos") instanceof ListTag) {
								ListTag pos = NbtHelper.tagFromCompound(meetingPoint, "pos");
								NbtHelper.applyOffsetToIntListPos(pos, offset);
							}
						}
						CompoundTag home = NbtHelper.tagFromCompound(memories, "minecraft:home");
						if (home != null && home.containsKey("pos")) {
							if (home.get("pos") instanceof IntArrayTag) {
								IntArrayTag pos = NbtHelper.tagFromCompound(home, "pos");
								NbtHelper.applyOffsetToIntArrayPos(pos, offset);
							} else if (home.get("pos") instanceof ListTag) {
								ListTag pos = NbtHelper.tagFromCompound(home, "pos");
								NbtHelper.applyOffsetToIntListPos(pos, offset);
							}
						}
						CompoundTag jobSite = NbtHelper.tagFromCompound(memories, "minecraft:job_site");
						if (jobSite != null && jobSite.containsKey("pos")) {
							if (jobSite.get("pos") instanceof IntArrayTag) {
								IntArrayTag pos = NbtHelper.tagFromCompound(jobSite, "pos");
								NbtHelper.applyOffsetToIntArrayPos(pos, offset);
							} else if (jobSite.get("pos") instanceof ListTag) {
								ListTag pos = NbtHelper.tagFromCompound(jobSite, "pos");
								NbtHelper.applyOffsetToIntListPos(pos, offset);
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
			CompoundTag patrolTarget = NbtHelper.tagFromCompound(entity, "PatrolTarget");
			NbtHelper.applyIntOffsetIfRootPresent(patrolTarget, "X", "Y", "Z", offset);
			break;
		case "minecraft:falling_block":
			CompoundTag tileEntityData = NbtHelper.tagFromCompound(entity, "TileEntityData");
			applyOffsetToTileEntity(tileEntityData, offset);
			break;
		}

		// recursively update passengers
		ListTag passengers = NbtHelper.tagFromCompound(entity, "Passengers");
		if (passengers != null) {
			passengers.forEach(p -> applyOffsetToEntity((CompoundTag) p, offset));
		}

		NbtHelper.fixEntityUUID(entity);
	}

	private void applyOffsetToTick(CompoundTag tick, Point3i offset) {
		NbtHelper.applyIntOffsetIfRootPresent(tick, "x", "y", "z", offset);
	}

	private void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
		if (tileEntity == null) {
			return;
		}

		NbtHelper.applyIntOffsetIfRootPresent(tileEntity, "x", "y", "z", offset);

		String id = NbtHelper.stringFromCompound(tileEntity, "id", "");
		switch (id) {
			case "minecraft:end_gateway" -> {
				CompoundTag exitPortal = NbtHelper.tagFromCompound(tileEntity, "ExitPortal");
				NbtHelper.applyIntOffsetIfRootPresent(exitPortal, "X", "Y", "Z", offset);
			}
			case "minecraft:structure_block" -> NbtHelper.applyIntOffsetIfRootPresent(tileEntity, "posX", "posY", "posZ", offset);
			case "minecraft:mob_spawner" -> {
				ListTag spawnPotentials = NbtHelper.tagFromCompound(tileEntity, "SpawnPotentials");
				if (spawnPotentials != null) {
					for (CompoundTag spawnPotential : spawnPotentials.iterateType(CompoundTag.TYPE)) {
						CompoundTag entity = NbtHelper.tagFromCompound(spawnPotential, "Entity");
						applyOffsetToEntity(entity, offset);
					}
				}
			}
		}
	}
}
