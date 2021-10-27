package net.querz.mcaselector.version.anvil118;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntArrayTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;

public final class LegacyHelper {

	private LegacyHelper() {}

	static ListTag<CompoundTag> getSections(CompoundTag root, int dataVersion) {
		if (dataVersion > 2843) { // 21w42a
			return Helper.tagFromCompound(root, "sections");
		} else {
			return Helper.tagFromLevelFromRoot(root, "Sections");
		}
	}

	static void putSections(CompoundTag root, ListTag<CompoundTag> sections, int dataVersion) {
		if (dataVersion > 2843) { // 21w42a
			root.put("sections", sections);
		} else {
			root.getCompoundTag("Level").put("Sections", sections);
		}
	}

	static ListTag<CompoundTag> getPalette(CompoundTag section, int dataVersion) {
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

	static String getStatus(CompoundTag root, int dataVersion) {
		if (dataVersion > 2843) { // 21w42a
			return Helper.stringFromCompound(root, "Status");
		} else {
			StringTag t = Helper.tagFromLevelFromRoot(root, "Status");
			if (t != null) {
				return t.getValue();
			}
		}
		return null;
	}

	static ListTag<CompoundTag> getTileEntities(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromLevelFromRoot(root, "TileEntities");
		} else {
			return Helper.tagFromCompound(root, "block_entities");
		}
	}

	static ListTag<CompoundTag> getTileTicks(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromLevelFromRoot(root, "TileTicks");
		} else {
			return Helper.tagFromCompound(root, "block_ticks");
		}
	}

	static ListTag<CompoundTag> getLiquidTicks(CompoundTag root, int dataVersion) {
		if (dataVersion < 2844) {
			return Helper.tagFromLevelFromRoot(root, "LiquidTicks");
		} else {
			return Helper.tagFromCompound(root, "fluid_ticks");
		}
	}

	static void putTileEntities(CompoundTag root, ListTag<CompoundTag> tileEntities, int dataVersion) {
		if (dataVersion < 2844) {
			root.getCompoundTag("Level").put("TileEntities", tileEntities);
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
			root.putInt("xPos", root.getInt("xPos") + offset.blockToChunk().getX());
			root.putInt("zPos", root.getInt("zPos") + offset.blockToChunk().getZ());
		} else {
			CompoundTag level = Helper.levelFromRoot(root);
			level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
			level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());
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
}
