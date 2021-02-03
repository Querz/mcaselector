package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.version.anvil112.Anvil112EntityRelocator.*;

public class Anvil112ChunkRelocator implements ChunkRelocator {

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

		return true;
	}

	private static void applyOffsetToTick(CompoundTag tick, Point2i offset) {
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
					applyIntIfPresent(tileEntity, "posZ", offset.getZ());
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
