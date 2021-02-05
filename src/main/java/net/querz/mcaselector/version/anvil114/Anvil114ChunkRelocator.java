package net.querz.mcaselector.version.anvil114;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;

public class Anvil114ChunkRelocator implements ChunkRelocator {

	@Override
	public boolean relocateChunk(CompoundTag root, Point2i offset) {
		if (root == null || !root.containsKey("Level")) {
			return false;
		}

		CompoundTag level = catchClassCastException(() -> root.getCompoundTag("Level"));
		if (level == null) {
			return true;
		}

		// adjust or set chunk position
		level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
		level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());

		// adjust entity positions
		if (level.containsKey("Entities") && level.get("Entities").getID() != LongArrayTag.ID) {
			ListTag<CompoundTag> entities = catchClassCastException(() -> level.getListTag("Entities").asCompoundTagList());
			if (entities != null) {
				entities.forEach(v -> applyOffsetToEntity(v, offset));
			}
		}

		// adjust tile entity positions
		if (level.containsKey("TileEntities") && level.get("TileEntities").getID() != LongArrayTag.ID) {
			ListTag<CompoundTag> tileEntities = catchClassCastException(() -> level.getListTag("TileEntities").asCompoundTagList());
			if (tileEntities != null) {
				tileEntities.forEach(v -> applyOffsetToTileEntity(v, offset));
			}
		}

		// adjust tile ticks
		if (level.containsKey("TileTicks")) {
			ListTag<CompoundTag> tileTicks = catchClassCastException(() -> level.getListTag("TileTicks").asCompoundTagList());
			if (tileTicks != null) {
				tileTicks.forEach(v -> applyOffsetToTick(v, offset));
			}
		}

		// adjust liquid ticks
		if (level.containsKey("LiquidTicks")) {
			ListTag<CompoundTag> liquidTicks = catchClassCastException(() -> level.getListTag("LiquidTicks").asCompoundTagList());
			if (liquidTicks != null) {
				liquidTicks.forEach(v -> applyOffsetToTick(v, offset));
			}
		}

		// adjust structures
		if (level.containsKey("Structures")) { // 1.13
			CompoundTag structures = catchClassCastException(() -> level.getCompoundTag("Structures"));
			if (structures != null) {
				applyOffsetToStructures(structures, offset);
			}
		}

		return true;
	}

	private void applyOffsetToEntity(CompoundTag entity, Point2i offset) {
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

		fixEntityUUID(entity);
	}

	private void fixEntityUUID(CompoundTag entity) {
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

	private void applyOffsetToStructures(CompoundTag structures, Point2i offset) { // 1.13
		Point2i chunkOffset = offset.blockToChunk();

		// update references
		if (structures.containsKey("References")) {
			CompoundTag references = catchClassCastException(() -> structures.getCompoundTag("References"));
			if (references != null) {
				for (Map.Entry<String, Tag<?>> entry : references) {
					long[] reference = catchClassCastException(() -> ((LongArrayTag) entry.getValue()).getValue());
					if (reference != null) {
						for (int i = 0; i < reference.length; i++) {
							int x = (int) (reference[i]);
							int z = (int) (reference[i] >> 32);
							reference[i] = ((long) (z + chunkOffset.getZ()) & 0xFFFFFFFFL) << 32 | (long) (x + chunkOffset.getX()) & 0xFFFFFFFFL;
						}
					}
				}
			}
		}

		// update starts
		if (structures.containsKey("Starts")) {
			CompoundTag starts = catchClassCastException(() -> structures.getCompoundTag("Starts"));
			if (starts != null) {
				for (Map.Entry<String, Tag<?>> entry : starts) {
					CompoundTag structure = catchClassCastException(() -> (CompoundTag) entry.getValue());
					if (structure == null || "INVALID".equals(catchClassCastException(() -> structure.getString("id")))) {
						continue;
					}
					applyIntIfPresent(structure, "ChunkX", chunkOffset.getX());
					applyIntIfPresent(structure, "ChunkZ", chunkOffset.getZ());
					applyOffsetToBB(catchClassCastException(() -> structure.getIntArray("BB")), offset);

					if (structure.containsKey("Processed")) {
						ListTag<CompoundTag> processed = catchClassCastException(() -> structure.getListTag("Processed").asCompoundTagList());
						if (processed != null) {
							for (CompoundTag chunk : processed) {
								applyIntIfPresent(chunk, "X", chunkOffset.getX());
								applyIntIfPresent(chunk, "Z", chunkOffset.getZ());
							}
						}
					}

					if (structure.containsKey("Children")) {
						ListTag<CompoundTag> children = catchClassCastException(() -> structure.getListTag("Children").asCompoundTagList());
						if (children != null) {
							for (CompoundTag child : children) {
								applyIntIfPresent(child, "TPX", offset.getX());
								applyIntIfPresent(child, "TPZ", offset.getZ());
								applyIntIfPresent(child, "PosX", offset.getX());
								applyIntIfPresent(child, "PosZ", offset.getZ());
								applyOffsetToBB(catchClassCastException(() -> child.getIntArray("BB")), offset);

								if (child.containsKey("Entrances")) {
									ListTag<IntArrayTag> entrances = catchClassCastException(() -> child.getListTag("Entrances").asIntArrayTagList());
									if (entrances != null) {
										entrances.forEach(e -> applyOffsetToBB(e.getValue(), offset));
									}
								}

								if (child.containsKey("junctions")) {
									ListTag<CompoundTag> junctions = catchClassCastException(() -> child.getListTag("junctions").asCompoundTagList());
									if (junctions != null) {
										for (CompoundTag junction : junctions) {
											applyIntIfPresent(junction, "source_x", offset.getX());
											applyIntIfPresent(junction, "source_z", offset.getZ());
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void applyOffsetToBB(int[] bb, Point2i offset) {
		if (bb == null || bb.length != 6) {
			return;
		}
		bb[0] += offset.getX();
		bb[2] += offset.getZ();
		bb[3] += offset.getX();
		bb[5] += offset.getZ();
	}

	private void applyOffsetToTick(CompoundTag tick, Point2i offset) {
		applyIntIfPresent(tick, "x", offset.getX());
		applyIntIfPresent(tick, "z", offset.getZ());
	}

	private void applyOffsetToTileEntity(CompoundTag tileEntity, Point2i offset) {
		if (tileEntity == null) {
			return;
		}

		applyIntIfPresent(tileEntity, "x", offset.getX());
		applyIntIfPresent(tileEntity, "z", offset.getZ());

		String id = catchClassCastException(() -> tileEntity.getString("id"));
		if (id != null) {
			switch (id) {
				case "minecraft:end_gateway":
					CompoundTag exitPortal = catchClassCastException(() -> tileEntity.getCompoundTag("ExitPortal"));
					applyIntOffsetIfRootPresent(exitPortal, "X", "Z", offset);
					break;
				case "minecraft:structure_block":
					applyIntIfPresent(tileEntity, "posX", offset.getX());
					applyIntIfPresent(tileEntity, "posX", offset.getZ());
					break;
				case "minecraft:mob_spawner":
					if (tileEntity.containsKey("SpawnPotentials")) {
						ListTag<CompoundTag> spawnPotentials = catchClassCastException(() -> tileEntity.getListTag("SpawnPotentials").asCompoundTagList());
						if (spawnPotentials != null) {
							for (CompoundTag spawnPotential : spawnPotentials) {
								CompoundTag entity = catchClassCastException(() -> spawnPotential.getCompoundTag("Entity"));
								if (entity != null) {
									applyOffsetToEntity(entity, offset);
								}
							}
						}
					}
			}
		}
	}

	private void applyIntOffsetIfRootPresent(CompoundTag root, String xKey, String zKey, Point2i offset) {
		if (root != null) {
			applyIntIfPresent(root, xKey, offset.getX());
			applyIntIfPresent(root, zKey, offset.getZ());
		}
	}

	private void applyIntIfPresent(CompoundTag root, String key, int offset) {
		Integer value;
		if (root.containsKey(key) && (value = catchClassCastException(() -> root.getInt(key))) != null) {
			root.putInt(key, value + offset);
		}
	}

	private void applyOffsetToIntListPos(ListTag<IntTag> pos, Point2i offset) {
		if (pos != null && pos.size() == 3) {
			pos.set(0, new IntTag(pos.get(0).asInt() + offset.getX()));
			pos.set(2, new IntTag(pos.get(2).asInt() + offset.getZ()));
		}
	}

	private void applyOffsetToIntArrayPos(int[] pos, Point2i offset) {
		if (pos != null && pos.length == 3) {
			pos[0] += offset.getX();
			pos[2] += offset.getZ();
		}
	}
}
