package net.querz.mcaselector.version.anvil118;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.*;

final class Snapshot118Helper {

	private static final int SNAPSHOT_21w43a = 2844;
	private static final int SNAPSHOT_21w37a = 2834;

	private Snapshot118Helper() {}

	static ListTag getProtoEntities(CompoundTag root, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(root, "entities");
		} else {
			return NbtHelper.tagFromLevelFromRoot(root, "Entities");
		}
	}

	static ListTag getSections(CompoundTag root, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(root, "sections");
		} else {
			return NbtHelper.getSectionsFromLevelFromRoot(root, "Sections");
		}
	}

	static void putSections(CompoundTag root, ListTag sections, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			root.put("sections", sections);
		} else {
			root.getCompound("Level").put("Sections", sections);
		}
	}

	static ListTag getPalette(CompoundTag section, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w37a) {
			return NbtHelper.tagFromCompound(NbtHelper.tagFromCompound(section, "block_states"), "palette");
		} else {
			return NbtHelper.tagFromCompound(section, "Palette");
		}
	}

	static long[] getBlockStates(CompoundTag section, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w37a) {
			return NbtHelper.longArrayFromCompound(NbtHelper.tagFromCompound(section, "block_states"), "data");
		} else {
			return NbtHelper.longArrayFromCompound(section, "BlockStates");
		}
	}

	static LongTag getInhabitedTime(CompoundTag root, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(root, "InhabitedTime");
		} else {
			return NbtHelper.tagFromLevelFromRoot(root, "InhabitedTime");
		}
	}

	static void setInhabitedTime(CompoundTag root, long inhabitedTime, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			if (root != null) {
				root.putLong("InhabitedTime", inhabitedTime);
			}
		} else {
			CompoundTag level = NbtHelper.levelFromRoot(root);
			if (level != null) {
				level.putLong("InhabitedTime", inhabitedTime);
			}
		}
	}

	static LongTag getLastUpdate(CompoundTag root, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(root, "LastUpdate");
		} else {
			return NbtHelper.tagFromLevelFromRoot(root, "LastUpdate");
		}
	}

	static void setLastUpdate(CompoundTag root, long lastUpdate, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			if (root != null) {
				root.putLong("LastUpdate", lastUpdate);
			}
		} else {
			CompoundTag level = NbtHelper.levelFromRoot(root);
			if (level != null) {
				level.putLong("LastUpdate", lastUpdate);
			}
		}
	}

	static StringTag getStatus(CompoundTag root, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(root, "Status");
		} else {
			return NbtHelper.tagFromLevelFromRoot(root, "Status");
		}
	}

	static void setStatus(CompoundTag root, String status, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			if (root != null) {
				root.putString("Status", status);
			}
		} else {
			CompoundTag level = NbtHelper.levelFromRoot(root);
			if (level != null) {
				level.putString("Status", status);
			}
		}
	}

	static ListTag getTileEntities(CompoundTag root, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			return NbtHelper.tagFromLevelFromRoot(root, "TileEntities");
		} else {
			return NbtHelper.tagFromCompound(root, "block_entities");
		}
	}

	static ListTag getTileTicks(CompoundTag root, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			return NbtHelper.tagFromLevelFromRoot(root, "TileTicks");
		} else {
			return NbtHelper.tagFromCompound(root, "block_ticks");
		}
	}

	static ListTag getLiquidTicks(CompoundTag root, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			return NbtHelper.tagFromLevelFromRoot(root, "LiquidTicks");
		} else {
			return NbtHelper.tagFromCompound(root, "fluid_ticks");
		}
	}

	static void putTileEntities(CompoundTag root, ListTag tileEntities, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			root.getCompound("Level").put("TileEntities", tileEntities);
		} else {
			root.put("block_entities", tileEntities);
		}
	}

	static Point2i getChunkCoordinates(CompoundTag root, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			return NbtHelper.point2iFromCompound(NbtHelper.tagFromCompound(root, "Level"), "xPos", "zPos");
		} else {
			return NbtHelper.point2iFromCompound(root, "xPos", "zPos");
		}
	}

	static void applyOffsetToChunkCoordinates(CompoundTag root, Point3i offset, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			CompoundTag level = NbtHelper.levelFromRoot(root);
			level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
			level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());
		} else {
			root.putInt("xPos", root.getInt("xPos") + offset.blockToChunk().getX());
			root.putInt("zPos", root.getInt("zPos") + offset.blockToChunk().getZ());
		}
	}

	static int[] getLegacyBiomes(CompoundTag root, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			IntArrayTag t = NbtHelper.tagFromLevelFromRoot(root, "Biomes");
			if (t != null) {
				return t.getValue();
			}
		}
		return null;
	}

	static CompoundTag getStructureStarts(CompoundTag root, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(NbtHelper.tagFromLevelFromRoot(root, "Structures", new CompoundTag()), "Starts", new CompoundTag());
		} else {
			return NbtHelper.tagFromCompound(NbtHelper.tagFromCompound(root, "structures"), "starts", new CompoundTag());
		}
	}

	static CompoundTag getStructures(CompoundTag root, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			return NbtHelper.tagFromLevelFromRoot(root, "Structures");
		} else {
			return NbtHelper.tagFromCompound(root, "structures");
		}
	}

	static CompoundTag getStructureStartsFromStructures(CompoundTag structures, int dataVersion) {
		if (dataVersion < SNAPSHOT_21w43a) {
			return NbtHelper.tagFromLevelFromRoot(structures, "Starts");
		} else {
			return NbtHelper.tagFromCompound(structures, "starts");
		}
	}

	static IntTag getXPos(CompoundTag data, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(data, "xPos");
		} else {
			return NbtHelper.tagFromLevelFromRoot(data, "xPos");
		}
	}

	static IntTag getYPos(CompoundTag data, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(data, "yPos");
		} else {
			return null;
		}
	}

	static IntTag getZPos(CompoundTag data, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(data, "zPos");
		} else {
			return NbtHelper.tagFromLevelFromRoot(data, "zPos");
		}
	}

	static ByteTag getIsLightOn(CompoundTag data, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			return NbtHelper.tagFromCompound(data, "isLightOn");
		} else {
			return NbtHelper.tagFromLevelFromRoot(data, "isLightOn");
		}
	}

	static void setIsLightOn(CompoundTag root, byte isLightOn, int dataVersion) {
		if (dataVersion >= SNAPSHOT_21w43a) {
			if (root != null) {
				root.putByte("isLightOn", isLightOn);
			}
		} else {
			CompoundTag level = NbtHelper.levelFromRoot(root);
			if (level != null) {
				level.putByte("isLightOn", isLightOn);
			}
		}
	}

}
