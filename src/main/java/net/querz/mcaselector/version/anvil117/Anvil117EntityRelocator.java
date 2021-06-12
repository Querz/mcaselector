package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.EntityRelocator;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.version.anvil117.Anvil117ChunkRelocator.*;

public class Anvil117EntityRelocator implements EntityRelocator {

	@Override
	public boolean relocateEntities(CompoundTag root, Point2i offset) {
		if (root == null) {
			return false;
		}

		int[] position = catchClassCastException(() -> root.getIntArray("Position"));
		if (position == null || position.length != 2) {
			return false;
		}

		position[0] += offset.blockToChunk().getX();
		position[1] += offset.blockToChunk().getZ();

		if (root.containsKey("Entities")) {
			ListTag<CompoundTag> entities = catchClassCastException(() -> root.getListTag("Entities").asCompoundTagList());
			if (entities != null) {
				entities.forEach(v -> applyOffsetToEntity(v, offset));
			}
		}
		return true;
	}

	static void applyOffsetToEntity(CompoundTag entity, Point2i offset) {
		if (entity == null) {
			return;
		}
		if (entity.containsKey("Pos")) {
			ListTag<DoubleTag> entityPos = catchClassCastException(() -> entity.getListTag("Pos").asDoubleTagList());
			if (entityPos != null && entityPos.size() == 3) {
				entityPos.set(0, new DoubleTag(entityPos.get(0).asDouble() + offset.getX()));
				entityPos.set(2, new DoubleTag(entityPos.get(2).asDouble() + offset.getZ()));
			}
		}

		// leashed entities
		if (entity.containsKey("Leash")) {
			CompoundTag leash = catchClassCastException(() -> entity.getCompoundTag("Leash"));
			applyIntOffsetIfRootPresent(leash, "X", "Z", offset);
		}

		// projectiles
		applyIntIfPresent(entity, "xTile", offset.getX());
		applyIntIfPresent(entity, "zTile", offset.getZ());

		// entities that have a sleeping place
		applyIntIfPresent(entity, "SleepingX", offset.getX());
		applyIntIfPresent(entity, "SleepingZ", offset.getZ());

		// positions for specific entity types
		String id = catchClassCastException(() -> entity.getString("id"));
		if (id != null) {
			switch (id) {
				case "minecraft:dolphin":
					if (entity.getBoolean("CanFindTreasure")) {
						applyIntIfPresent(entity, "TreasurePosX", offset.getX());
						applyIntIfPresent(entity, "TreasurePosZ", offset.getZ());
					}
					break;
				case "minecraft:phantom":
					applyIntIfPresent(entity, "AX", offset.getX());
					applyIntIfPresent(entity, "AZ", offset.getZ());
					break;
				case "minecraft:shulker":
					applyIntIfPresent(entity, "APX", offset.getX());
					applyIntIfPresent(entity, "APZ", offset.getZ());
					break;
				case "minecraft:turtle":
					applyIntIfPresent(entity, "HomePosX", offset.getX());
					applyIntIfPresent(entity, "HomePosZ", offset.getZ());
					applyIntIfPresent(entity, "TravelPosX", offset.getX());
					applyIntIfPresent(entity, "TravelPosZ", offset.getZ());
					break;
				case "minecraft:vex":
					applyIntIfPresent(entity, "BoundX", offset.getX());
					applyIntIfPresent(entity, "BoundZ", offset.getZ());
					break;
				case "minecraft:wandering_trader":
					if (entity.containsKey("WanderTarget")) {
						CompoundTag wanderTarget = catchClassCastException(() -> entity.getCompoundTag("WanderTarget"));
						applyIntOffsetIfRootPresent(wanderTarget, "X", "Z", offset);
					}
					break;
				case "minecraft:shulker_bullet":
					CompoundTag owner = catchClassCastException(() -> entity.getCompoundTag("Owner"));
					applyIntOffsetIfRootPresent(owner, "X", "Z", offset);
					CompoundTag target = catchClassCastException(() -> entity.getCompoundTag("Target"));
					applyIntOffsetIfRootPresent(target, "X", "Z", offset);
					break;
				case "minecraft:end_crystal":
					CompoundTag beamTarget = catchClassCastException(() -> entity.getCompoundTag("BeamTarget"));
					applyIntOffsetIfRootPresent(beamTarget, "X", "Z", offset);
					break;
				case "minecraft:item_frame":
				case "minecraft:painting":
					applyIntIfPresent(entity, "TileX", offset.getX());
					applyIntIfPresent(entity, "TileZ", offset.getZ());
					break;
				case "minecraft:villager":
					if (entity.containsKey("Brain")) {
						CompoundTag brain = catchClassCastException(() -> entity.getCompoundTag("Brain"));
						if (brain != null && brain.containsKey("memories")) {
							CompoundTag memories = catchClassCastException(() -> brain.getCompoundTag("memories"));
							if (memories != null && memories.size() > 0) {
								CompoundTag meetingPoint = catchClassCastException(() -> memories.getCompoundTag("minecraft:meeting_point"));
								if (meetingPoint != null && meetingPoint.containsKey("pos")) {
									if (meetingPoint.get("pos") instanceof IntArrayTag) {
										int[] pos = catchClassCastException(() -> meetingPoint.getIntArray("pos"));
										applyOffsetToIntArrayPos(pos, offset);
									} else if (meetingPoint.get("pos") instanceof ListTag) {
										ListTag<IntTag> pos = catchClassCastException(() -> meetingPoint.getListTag("pos").asIntTagList());
										applyOffsetToIntListPos(pos, offset);
									}
								}
								CompoundTag home = catchClassCastException(() -> memories.getCompoundTag("minecraft:home"));
								if (home != null && home.containsKey("pos")) {
									if (home.get("pos") instanceof IntArrayTag) {
										int[] pos = catchClassCastException(() -> home.getIntArray("pos"));
										applyOffsetToIntArrayPos(pos, offset);
									} else if (home.get("pos") instanceof ListTag) {
										ListTag<IntTag> pos = catchClassCastException(() -> home.getListTag("pos").asIntTagList());
										applyOffsetToIntListPos(pos, offset);
									}
								}
								CompoundTag jobSite = catchClassCastException(() -> memories.getCompoundTag("minecraft:job_site"));
								if (jobSite != null && jobSite.containsKey("pos")) {
									if (jobSite.get("pos") instanceof IntArrayTag) {
										int[] pos = catchClassCastException(() -> jobSite.getIntArray("pos"));
										applyOffsetToIntArrayPos(pos, offset);
									} else if (jobSite.get("pos") instanceof ListTag) {
										ListTag<IntTag> pos = catchClassCastException(() -> jobSite.getListTag("pos").asIntTagList());
										applyOffsetToIntListPos(pos, offset);
									}
								}
							}
						}
					}
					break;
				case "minecraft:pillager":
				case "minecraft:witch":
				case "minecraft:vindicator":
				case "minecraft:ravager":
				case "minecraft:illusioner":
				case "minecraft:evoker":
					CompoundTag patrolTarget = catchClassCastException(() -> entity.getCompoundTag("PatrolTarget"));
					if (patrolTarget != null) {
						applyIntOffsetIfRootPresent(patrolTarget, "X", "Z", offset);
					}
					break;
				case "minecraft:falling_block":
					CompoundTag tileEntityData = catchClassCastException(() -> entity.getCompoundTag("TileEntityData"));
					applyOffsetToTileEntity(tileEntityData, offset);
			}
		}

		// recursively update passengers
		if (entity.containsKey("Passengers")) {
			ListTag<CompoundTag> passengers = catchClassCastException(() -> entity.getListTag("Passengers").asCompoundTagList());
			if (passengers != null) {
				passengers.forEach(p -> applyOffsetToEntity(p, offset));
			}
		}

		if (entity.containsKey("Item")) {
			CompoundTag item = catchClassCastException(() -> entity.getCompoundTag("Item"));
			applyOffsetToItem(item, offset);
		}

		if (entity.containsKey("Items")) {
			ListTag<CompoundTag> items = catchClassCastException(() -> entity.getListTag("Items").asCompoundTagList());
			if (items != null) {
				items.forEach(i -> applyOffsetToItem(i, offset));
			}
		}

		if (entity.containsKey("HandItems")) {
			ListTag<CompoundTag> items = catchClassCastException(() -> entity.getListTag("HandItems").asCompoundTagList());
			if (items != null) {
				items.forEach(i -> applyOffsetToItem(i, offset));
			}
		}

		if (entity.containsKey("ArmorItems")) {
			ListTag<CompoundTag> items = catchClassCastException(() -> entity.getListTag("ArmorItems").asCompoundTagList());
			if (items != null) {
				items.forEach(i -> applyOffsetToItem(i, offset));
			}
		}

		fixEntityUUID(entity);
	}

	private static void fixEntityUUID(CompoundTag entity) {
		if (entity.containsKey("UUIDMost")) {
			entity.putLong("UUIDMost", random.nextLong());
		}
		if (entity.containsKey("UUIDLeast")) {
			entity.putLong("UUIDLeast", random.nextLong());
		}
		if (entity.containsKey("UUID")) {
			int[] uuid = entity.getIntArray("UUID");
			if (uuid.length == 4) {
				for (int i = 0; i < 4; i++) {
					uuid[i] = random.nextInt();
				}
			}
		}
	}
}
