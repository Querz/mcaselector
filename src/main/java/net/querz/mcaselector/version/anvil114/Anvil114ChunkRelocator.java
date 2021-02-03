package net.querz.mcaselector.version.anvil114;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.version.anvil114.Anvil114EntityRelocator.*;

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

	static void applyOffsetToTileEntity(CompoundTag tileEntity, Point2i offset) {
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

	static void applyIntOffsetIfRootPresent(CompoundTag root, String xKey, String zKey, Point2i offset) {
		if (root != null) {
			applyIntIfPresent(root, xKey, offset.getX());
			applyIntIfPresent(root, zKey, offset.getZ());
		}
	}

	static void applyIntIfPresent(CompoundTag root, String key, int offset) {
		Integer value;
		if (root.containsKey(key) && (value = catchClassCastException(() -> root.getInt(key))) != null) {
			root.putInt(key, value + offset);
		}
	}

	static void applyOffsetToIntListPos(ListTag<IntTag> pos, Point2i offset) {
		if (pos != null && pos.size() == 3) {
			pos.set(0, new IntTag(pos.get(0).asInt() + offset.getX()));
			pos.set(2, new IntTag(pos.get(2).asInt() + offset.getZ()));
		}
	}

	static void applyOffsetToIntArrayPos(int[] pos, Point2i offset) {
		if (pos != null && pos.length == 3) {
			pos[0] += offset.getX();
			pos[2] += offset.getZ();
		}
	}
}
