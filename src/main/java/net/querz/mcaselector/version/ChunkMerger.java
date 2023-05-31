package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import java.util.*;
import java.util.function.Function;

public interface ChunkMerger {

	void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset);

	CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion);

	default ListTag mergeLists(ListTag source, ListTag destination, List<Range> ranges, Function<Tag, Integer> ySupplier, int yOffset) {
		ListTag result = new ListTag();
		for (Tag dest : destination) {
			int y = ySupplier.apply(dest);
			for (Range range : ranges) {
				if (!range.contains(y)) {
					result.add(dest);
				}
			}
		}

		for (Tag sourceElement : source) {
			int y = ySupplier.apply(sourceElement);
			for (Range range : ranges) {
				if (range.contains(y - yOffset)) {
					result.add(sourceElement);
					break;
				}
			}
		}

		return result;
	}

	default void mergeListTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name) {
		ListTag sourceList = Helper.tagFromLevelFromRoot(source, name);
		ListTag destinationList = Helper.tagFromLevelFromRoot(destination, name, sourceList);

		if (sourceList == null || destinationList == null || sourceList.size() != destinationList.size()) {
			return;
		}

		for (Range range : ranges) {
			int m = Math.min(range.getTo() + yOffset, sourceList.size() - 1);
			for (int i = Math.max(range.getFrom() + yOffset, 0); i <= m; i++) {
				destinationList.set(i, sourceList.get(i));
			}
		}

		initLevel(destination).put(name, destinationList);
	}

	default void mergeCompoundTagListsFromLevel(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<Tag, Integer> ySupplier) {
		ListTag sourceElements = Helper.tagFromLevelFromRoot(source, name, new ListTag());
		ListTag destinationElements = Helper.tagFromLevelFromRoot(destination, name, new ListTag());

		initLevel(destination).put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
	}

	default void mergeCompoundTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<Tag, Integer> ySupplier) {
		ListTag sourceElements = Helper.tagFromCompound(source, name, new ListTag());
		ListTag destinationElements = Helper.tagFromCompound(destination, name, new ListTag());

		destination.put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
	}

	// merge based on compound tag keys, assuming compound tag keys are ints
	default void mergeCompoundTags(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name) {
		CompoundTag sourceElements = Helper.tagFromCompound(source, name, new CompoundTag());
		CompoundTag destinationElements = Helper.tagFromCompound(destination, name, new CompoundTag());

		for (Map.Entry<String, Tag> sourceElement : sourceElements) {
			if (sourceElement.getKey().matches("^-?[0-9]{1,2}$")) {
				int y = Integer.parseInt(sourceElement.getKey());
				for (Range range : ranges) {
					if (range.contains(y - yOffset)) {
						destinationElements.put(sourceElement.getKey(), sourceElement.getValue());
						break;
					}
				}
			}
		}
	}

	default CompoundTag initLevel(CompoundTag c) {
		CompoundTag level = Helper.levelFromRoot(c);
		if (level == null) {
			c.put("Level", level = new CompoundTag());
		}
		return level;
	}

	default void fixEntityUUIDs(CompoundTag root) {
		ListTag entities = Helper.tagFromCompound(root, "Entities", null);
		if (entities != null) {
			entities.forEach(e -> fixEntityUUID((CompoundTag) e));
		}
	}

	private static void fixEntityUUID(CompoundTag entity) {
		Helper.fixEntityUUID(entity);
		if (entity.containsKey("Passengers")) {
			ListTag passengers = Helper.tagFromCompound(entity, "Passengers", null);
			if (passengers != null) {
				passengers.forEach(e -> fixEntityUUID((CompoundTag) e));
			}
		}
	}
}
