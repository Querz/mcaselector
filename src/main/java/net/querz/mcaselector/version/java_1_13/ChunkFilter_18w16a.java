package net.querz.mcaselector.version.java_1_13;

import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.*;
import java.util.List;

import static net.querz.mcaselector.util.validation.ValidationHelper.catchAndLog;

public class ChunkFilter_18w16a {

	@MCVersionImplementation(1483)
	public static class Merge extends ChunkFilter_18w06a.Merge {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Sections", c -> ((CompoundTag) c).getInt("Y"));
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Entities", c -> ((CompoundTag) c).getList("Pos").getInt(1) >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileEntities", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "LiquidTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeListTagLists(source, destination, ranges, yOffset, "Lights");
			mergeListTagLists(source, destination, ranges, yOffset, "LiquidsToBeTicked");
			mergeListTagLists(source, destination, ranges, yOffset, "ToBeTicked");
			mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");
			mergeStructures(source, destination, ranges, yOffset);

			// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
			fixEntityUUIDs(Helper.levelFromRoot(destination));
		}
	}

	@MCVersionImplementation(1483)
	public static class Relocate extends ChunkFilter_18w06a.Relocate {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			CompoundTag level = Helper.tagFromCompound(root, "Level");
			if (level == null) {
				return false;
			}

			// adjust or set chunk position
			level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
			level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());

			// adjust entity positions
			ListTag entities = Helper.tagFromCompound(level, "Entities");
			if (entities != null) {
				entities.forEach(v -> catchAndLog(() -> applyOffsetToEntity((CompoundTag) v, offset)));
			}

			// adjust tile entity positions
			ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities");
			if (tileEntities != null) {
				tileEntities.forEach(v -> catchAndLog(() -> applyOffsetToTileEntity((CompoundTag) v, offset)));
			}

			// adjust tile ticks
			ListTag tileTicks = Helper.tagFromCompound(level, "TileTicks");
			if (tileTicks != null) {
				tileTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// adjust liquid ticks
			ListTag liquidTicks = Helper.tagFromCompound(level, "LiquidTicks");
			if (liquidTicks != null) {
				liquidTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// adjust structures
			CompoundTag structures = Helper.tagFromCompound(level, "Structures");
			if (structures != null) {
				catchAndLog(() -> applyOffsetToStructures(structures, offset));
			}

			// Lights
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "Lights", offset.blockToSection()));

			// LiquidsToBeTicked
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "LiquidsToBeTicked", offset.blockToSection()));

			// ToBeTicked
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "ToBeTicked", offset.blockToSection()));

			// PostProcessing
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "PostProcessing", offset.blockToSection()));

			// adjust sections vertically
			ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
			if (sections != null) {
				ListTag newSections = new ListTag();
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					if (applyOffsetToSection(section, offset.blockToSection(), new Range(0, 15))) {
						newSections.add(section);
					}
				}
				level.put("Sections", newSections);
			}

			return true;
		}
	}
}
