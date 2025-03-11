package net.querz.mcaselector.version.java_1_14;

import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_13.ChunkFilter_18w16a;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.DoubleTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.ListTag;

import static net.querz.mcaselector.util.validation.ValidationHelper.attempt;

public class ChunkFilter_19w11a {

	@MCVersionImplementation(1937)
	public static class Relocate extends ChunkFilter_18w16a.Relocate {

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

		protected void applyOffsetToVillagerMemory(CompoundTag memory, Point3i offset) {
			IntArrayTag mPos = Helper.tagFromCompound(memory, "pos");
			Helper.applyOffsetToIntArrayPos(mPos, offset);
			if (mPos == null) {
				ListTag lPos = Helper.tagFromCompound(memory, "pos");
				Helper.applyOffsetToIntListPos(lPos, offset);
			}
		}
	}
}
