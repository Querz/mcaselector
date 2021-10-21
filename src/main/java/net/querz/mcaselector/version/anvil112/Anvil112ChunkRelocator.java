package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.NumberTag;

public class Anvil112ChunkRelocator implements ChunkRelocator {

	public boolean relocateChunk(CompoundTag root, Point3i offset) {
		CompoundTag level = Helper.tagFromCompound(root, "Level");
		if (level == null) {
			return false;
		}

		// adjust or set chunk position
		level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
		level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());

		// adjust entity positions
		ListTag<CompoundTag> entities = Helper.tagFromCompound(level, "Entities");
		if (entities != null) {
			entities.forEach(v -> applyOffsetToEntity(v, offset));
		}

		// adjust tile entity positions
		ListTag<CompoundTag> tileEntities = Helper.tagFromCompound(level, "TileEntities");
		if (tileEntities != null) {
			tileEntities.forEach(v -> applyOffsetToTileEntity(v, offset));
		}

		// adjust tile ticks
		ListTag<CompoundTag> tileTicks = Helper.tagFromCompound(level, "TileTicks");
		if (tileTicks != null) {
			tileTicks.forEach(v -> applyOffsetToTick(v, offset));
		}

		// adjust liquid ticks
		ListTag<CompoundTag> liquidTicks = Helper.tagFromCompound(level, "LiquidTicks");
		if (liquidTicks != null) {
			liquidTicks.forEach(v -> applyOffsetToTick(v, offset));
		}

		// Lights
		Helper.applyOffsetToListOfShortTagLists(level, "Lights", offset);

		// LiquidsToBeTicked
		Helper.applyOffsetToListOfShortTagLists(level, "LiquidsToBeTicked", offset);

		// ToBeTicked
		Helper.applyOffsetToListOfShortTagLists(level, "ToBeTicked", offset);

		// PostProcessing
		Helper.applyOffsetToListOfShortTagLists(level, "PostProcessing", offset);

		// adjust sections vertically
		ListTag<CompoundTag> sections = Helper.tagFromLevelFromRoot(root, "Sections");
		if (sections != null) {
			ListTag<CompoundTag> newSections = new ListTag<>(CompoundTag.class);
			for (CompoundTag section : sections) {
				if (applyOffsetToSection(section, offset, 0, 15)) {
					newSections.add(section);
				}
			}
		}

		return true;
	}

	private void applyOffsetToEntity(CompoundTag entity, Point3i offset) {
		if (entity == null) {
			return;
		}

		ListTag<DoubleTag> entityPos = Helper.tagFromCompound(entity, "Pos");
		if (entityPos != null && entityPos.size() == 3) {
			entityPos.set(0, new DoubleTag(entityPos.get(0).asDouble() + offset.getX()));
			entityPos.set(1, new DoubleTag(entityPos.get(1).asDouble() + offset.getY()));
			entityPos.set(2, new DoubleTag(entityPos.get(2).asDouble() + offset.getZ()));
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
					if (memories != null && memories.size() > 0) {
						CompoundTag meetingPoint = Helper.tagFromCompound(memories, "minecraft:meeting_point");
						if (meetingPoint != null && meetingPoint.containsKey("pos")) {
							if (meetingPoint.get("pos") instanceof IntArrayTag) {
								IntArrayTag pos = Helper.tagFromCompound(meetingPoint, "pos");
								Helper.applyOffsetToIntArrayPos(pos, offset);
							} else if (meetingPoint.get("pos") instanceof ListTag) {
								ListTag<IntTag> pos = Helper.tagFromCompound(meetingPoint, "pos");
								Helper.applyOffsetToIntListPos(pos, offset);
							}
						}
						CompoundTag home = Helper.tagFromCompound(memories, "minecraft:home");
						if (home != null && home.containsKey("pos")) {
							if (home.get("pos") instanceof IntArrayTag) {
								IntArrayTag pos = Helper.tagFromCompound(home, "pos");
								Helper.applyOffsetToIntArrayPos(pos, offset);
							} else if (home.get("pos") instanceof ListTag) {
								ListTag<IntTag> pos = Helper.tagFromCompound(home, "pos");
								Helper.applyOffsetToIntListPos(pos, offset);
							}
						}
						CompoundTag jobSite = Helper.tagFromCompound(memories, "minecraft:job_site");
						if (jobSite != null && jobSite.containsKey("pos")) {
							if (jobSite.get("pos") instanceof IntArrayTag) {
								IntArrayTag pos = Helper.tagFromCompound(jobSite, "pos");
								Helper.applyOffsetToIntArrayPos(pos, offset);
							} else if (jobSite.get("pos") instanceof ListTag) {
								ListTag<IntTag> pos = Helper.tagFromCompound(jobSite, "pos");
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
		ListTag<CompoundTag> passengers = Helper.tagFromCompound(entity, "Passengers");
		if (passengers != null) {
			passengers.forEach(p -> applyOffsetToEntity(p, offset));
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
		case "minecraft:end_gateway":
			CompoundTag exitPortal = Helper.tagFromCompound(tileEntity, "ExitPortal");
			Helper.applyIntOffsetIfRootPresent(exitPortal, "X", "Y", "Z", offset);
			break;
		case "minecraft:structure_block":
			Helper.applyIntOffsetIfRootPresent(tileEntity, "posX", "posY", "posZ", offset);
			break;
		case "minecraft:mob_spawner":
			ListTag<CompoundTag> spawnPotentials = Helper.tagFromCompound(tileEntity, "SpawnPotentials");
			if (spawnPotentials != null) {
				for (CompoundTag spawnPotential : spawnPotentials) {
					CompoundTag entity = Helper.tagFromCompound(spawnPotential, "Entity");
					applyOffsetToEntity(entity, offset);
				}
			}
		}
	}
}
