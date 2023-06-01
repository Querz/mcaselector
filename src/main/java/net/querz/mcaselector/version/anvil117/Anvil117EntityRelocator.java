package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.*;
import static net.querz.mcaselector.version.anvil117.Anvil117ChunkRelocator.*;

public class Anvil117EntityRelocator implements ChunkRelocator {

	@Override
	public boolean relocate(CompoundTag root, Point3i offset) {
		int[] position = NbtHelper.intArrayFromCompound(root, "Position");
		if (position == null || position.length != 2) {
			return false;
		}

		position[0] += offset.blockToChunk().getX();
		position[1] += offset.blockToChunk().getZ();

		// adjust entity positions
		ListTag entities = NbtHelper.tagFromCompound(root, "Entities");
		if (entities != null) {
			entities.forEach(v -> applyOffsetToEntity((CompoundTag) v, offset));
		}

		return true;
	}

	public static void applyOffsetToEntity(CompoundTag entity, Point3i offset) {
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
		case "minecraft:dolphin":
			NbtHelper.applyIntOffsetIfRootPresent(entity, "TreasurePosX", "TreasurePosY", "TreasurePosZ", offset);
			break;
		case "minecraft:phantom":
			NbtHelper.applyIntOffsetIfRootPresent(entity, "AX", "AY", "AZ", offset);
			break;
		case "minecraft:shulker":
			NbtHelper.applyIntOffsetIfRootPresent(entity, "APX", "APY", "APZ", offset);
			break;
		case "minecraft:turtle":
			NbtHelper.applyIntOffsetIfRootPresent(entity, "HomePosX", "HomePosY", "HomePosZ", offset);
			NbtHelper.applyIntOffsetIfRootPresent(entity, "TravelPosX", "TravelPosY", "TravelPosZ", offset);
			break;
		case "minecraft:vex":
			NbtHelper.applyIntOffsetIfRootPresent(entity, "BoundX", "BoundY", "BoundZ", offset);
			break;
		case "minecraft:wandering_trader":
			CompoundTag wanderTarget = NbtHelper.tagFromCompound(entity, "WanderTarget");
			NbtHelper.applyIntOffsetIfRootPresent(wanderTarget, "X", "Y", "Z", offset);
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
			CompoundTag memories = NbtHelper.tagFromCompound(NbtHelper.tagFromCompound(entity, "Brain"), "memories");
			if (memories != null && memories.size() > 0) {
				applyOffsetToVillagerMemory(NbtHelper.tagFromCompound(memories, "minecraft:meeting_point"), offset);
				applyOffsetToVillagerMemory(NbtHelper.tagFromCompound(memories, "minecraft:home"), offset);
				applyOffsetToVillagerMemory(NbtHelper.tagFromCompound(memories, "minecraft:job_site"), offset);
			}
			break;
		case "minecraft:pillager":
		case "minecraft:witch":
		case "minecraft:vindicator":
		case "minecraft:ravager":
		case "minecraft:illusioner":
		case "minecraft:evoker":
			CompoundTag patrolTarget = NbtHelper.tagFromCompound(entity, "PatrolTarget");
			NbtHelper.applyIntOffsetIfRootPresent(patrolTarget, "X", "Y", "Z", offset);
			break;
		case "minecraft:falling_block":
			CompoundTag tileEntityData = NbtHelper.tagFromCompound(entity, "TileEntityData");
			applyOffsetToTileEntity(tileEntityData, offset);
		}

		// recursively update passengers
		ListTag passengers = NbtHelper.tagFromCompound(entity, "Passengers");
		if (passengers != null) {
			passengers.forEach(p -> applyOffsetToEntity((CompoundTag) p, offset));
		}

		CompoundTag item = NbtHelper.tagFromCompound(entity, "Item");
		applyOffsetToItem(item, offset);

		ListTag items = NbtHelper.tagFromCompound(entity, "Items");
		if (items != null) {
			items.forEach(i -> applyOffsetToItem((CompoundTag) i, offset));
		}

		ListTag handItems = NbtHelper.tagFromCompound(entity, "HandItems");
		if (handItems != null) {
			handItems.forEach(i -> applyOffsetToItem((CompoundTag) i, offset));
		}

		ListTag armorItems = NbtHelper.tagFromCompound(entity, "ArmorItems");
		if (armorItems != null) {
			armorItems.forEach(i -> applyOffsetToItem((CompoundTag) i, offset));
		}

		NbtHelper.fixEntityUUID(entity);
	}

	static void applyOffsetToVillagerMemory(CompoundTag memory, Point3i offset) {
		IntArrayTag mPos = NbtHelper.tagFromCompound(memory, "pos");
		NbtHelper.applyOffsetToIntArrayPos(mPos, offset);
		if (mPos == null) {
			ListTag lPos = NbtHelper.tagFromCompound(memory, "pos");
			NbtHelper.applyOffsetToIntListPos(lPos, offset);
		}
	}
}
