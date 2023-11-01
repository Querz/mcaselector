package net.querz.mcaselector.version.anvil118;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.*;

public final class LegacyHelper {

	private LegacyHelper() {}

	static ListTag getProtoEntities(CompoundTag root, int dataVersion) {
		if (dataVersion > 2843) {
			return Helper.tagFromCompound(root, "entities");
		} else {
			return Helper.tagFromLevelFromRoot(root, "Entities");
		}
	}

	static ListTag getEntities(CompoundTag root, int dataVersion) {
		if (dataVersion > 2843) {
			return Helper.tagFromCompound(root, "Entities");
		} else {
			return Helper.tagFromLevelFromRoot(root, "Entities");
		}
	}

	static ListTag getSections(CompoundTag root, int dataVersion) {
		if (dataVersion > 2843) { // 21w42a
			return Helper.tagFromCompound(root, "sections");
		} else {
			return Helper.getSectionsFromLevelFromRoot(root, "Sections");
		}
	}

	static void putSections(CompoundTag root, ListTag sections, int dataVersion) {
		if (dataVersion > 2843) { // 21w42a
			root.put("sections", sections);
		} else {
			root.getCompound("Level").put("Sections", sections);
		}
	}

	static ListTag getPalette(CompoundTag section, int dataVersion) {
		if (dataVersion >= 2834) {
			return Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
		} else {
			return Helper.tagFromCompound(section, "Palette");
		}
	}

	static long[] getBlockStates(CompoundTag section, int dataVersion) {
		if (dataVersion >= 2834) {
			return Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
		} else {
			return Helper.longArrayFromCompound(section, "BlockStates");
		}
	}

	static LongTag getInhabitedTime(CompoundTag root, int dataVersion) {
		if (dataVersion > 2843) { // 21w42a
			return Helper.tagFromCompound(root, "InhabitedTime");
		} else {
			return Helper.tagFromLevelFromRoot(root, "InhabitedTime");
		}
	}

	static void setInhabitedTime(CompoundTag root, long inhabitedTime, int dataVersion) {
		if (dataVersion > 2843) {
			if (root != null) {
				root.putLong("InhabitedTime", inhabitedTime);
			}
		} else {
			CompoundTag level = Helper.levelFromRoot(root);
			if (level != null) {
				level.putLong("InhabitedTime", inhabitedTime);
			}
		}
	}

	static LongTag getLastUpdate(CompoundTag root, int dataVersion) {
		if (dataVersion > 2843) { // 21w42a
			return Helper.tagFromCompound(root, "LastUpdate");
		} else {
			return Helper.tagFromLevelFromRoot(root, "LastUpdate");
		}
	}

	static void setLastUpdate(CompoundTag root, long lastUpdate, int dataVersion) {
		if (dataVersion > 2843) {
			if (root != null) {
				root.putLong("LastUpdate", lastUpdate);
			}
		} else {
			CompoundTag level = Helper.levelFromRoot(root);
			if (level != null) {
				level.putLong("LastUpdate", lastUpdate);
			}
		}
	}

	static StringTag getStatus(CompoundTag root, int dataVersion) {
		if (dataVersion > 2843) {
			return Helper.tagFromCompound(root, "Status");
		} else {
			return Helper.tagFromLevelFromRoot(root, "Status");
		}
	}

	static void setStatus(CompoundTag root, String status, int dataVersion) {
		if (dataVersion > 2843) {
			if (root != null) {
				root.putString("Status", status);
			}
		} else {
			CompoundTag level = Helper.levelFromRoot(root);
			if (level != null) {
				level.putString("Status", status);
			}
		}
	}

	static ListTag getTileEntities(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromLevelFromRoot(root, "TileEntities");
		} else {
			return Helper.tagFromCompound(root, "block_entities");
		}
	}

	static ListTag getTileTicks(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromLevelFromRoot(root, "TileTicks");
		} else {
			return Helper.tagFromCompound(root, "block_ticks");
		}
	}

	static ListTag getLiquidTicks(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromLevelFromRoot(root, "LiquidTicks");
		} else {
			return Helper.tagFromCompound(root, "fluid_ticks");
		}
	}

	static void putTileEntities(CompoundTag root, ListTag tileEntities, int dataVersion) {
		if (dataVersion < 2844) {
			root.getCompound("Level").put("TileEntities", tileEntities);
		} else {
			root.put("block_entities", tileEntities);
		}
	}

	static Point2i getChunkCoordinates(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.point2iFromCompound(Helper.tagFromCompound(root, "Level"), "xPos", "zPos");
		} else {
			return Helper.point2iFromCompound(root, "xPos", "zPos");
		}
	}

	static void applyOffsetToChunkCoordinates(CompoundTag root, Point3i offset, int dataVersion) {
		if (dataVersion < 2844) {
			CompoundTag level = Helper.levelFromRoot(root);
			level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
			level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());
		} else {
			root.putInt("xPos", root.getInt("xPos") + offset.blockToChunk().getX());
			root.putInt("zPos", root.getInt("zPos") + offset.blockToChunk().getZ());
		}
	}

	static int[] getLegacyBiomes(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) { // 21w43a
			IntArrayTag t = Helper.tagFromLevelFromRoot(root, "Biomes");
			if (t != null) {
				return t.getValue();
			}
		}
		return null;
	}

	static CompoundTag getStructureStarts(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromCompound(Helper.tagFromLevelFromRoot(root, "Structures", new CompoundTag()), "Starts", new CompoundTag());
		} else {
			return Helper.tagFromCompound(Helper.tagFromCompound(root, "structures"), "starts", new CompoundTag());
		}
	}

	static CompoundTag getStructures(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromLevelFromRoot(root, "Structures");
		} else {
			return Helper.tagFromCompound(root, "structures");
		}
	}

	static CompoundTag getStructureStartsFromStructures(CompoundTag structures, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromLevelFromRoot(structures, "Starts");
		} else {
			return Helper.tagFromCompound(structures, "starts");
		}
	}

	static IntTag getXPos(CompoundTag data, int dataVersion) {
		if (dataVersion > 2843) {
			return Helper.tagFromCompound(data, "xPos");
		} else {
			return Helper.tagFromLevelFromRoot(data, "xPos");
		}
	}

	static IntTag getYPos(CompoundTag data, int dataVersion) {
		if (dataVersion > 2843) {
			return Helper.tagFromCompound(data, "yPos");
		} else {
			return null;
		}
	}

	static CompoundTag getHeightmaps(CompoundTag data, int dataVersion) {
		if (dataVersion > 2843) {
			return data.getCompoundTag("Heightmaps");
		} else {
			return Helper.tagFromLevelFromRoot(data, "Heightmaps");
		}
	}

	static void setHeightmaps(CompoundTag data, CompoundTag heightmaps, int dataVersion) {
		if (dataVersion > 2843) {
			data.put("Heightmaps", heightmaps);
		} else {
			CompoundTag level = Helper.levelFromRoot(data);
			if (level != null) {
				level.put("Heightmaps", heightmaps);
			}
		}
	}

	static IntTag getZPos(CompoundTag data, int dataVersion) {
		if (dataVersion > 2843) {
			return Helper.tagFromCompound(data, "zPos");
		} else {
			return Helper.tagFromLevelFromRoot(data, "zPos");
		}
	}

	static ByteTag getIsLightOn(CompoundTag data, int dataVersion) {
		if (dataVersion > 2843) {
			return Helper.tagFromCompound(data, "isLightOn");
		} else {
			return Helper.tagFromLevelFromRoot(data, "isLightOn");
		}
	}

	static void setIsLightOn(CompoundTag root, byte isLightOn, int dataVersion) {
		if (dataVersion > 2843) {
			if (root != null) {
				root.putByte("isLightOn", isLightOn);
			}
		} else {
			CompoundTag level = Helper.levelFromRoot(root);
			if (level != null) {
				level.putByte("isLightOn", isLightOn);
			}
		}
	}
}
