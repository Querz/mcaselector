package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;

import java.util.*;
import java.util.function.Function;

public interface ChunkMerger {

	void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset);

	CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion);

	default <T extends Tag<?>> ListTag<T> mergeLists(ListTag<T> source, ListTag<T> destination, List<Range> ranges, Function<T, Integer> ySupplier, int yOffset) {

		Map<Integer, T> resultSet = new HashMap<>();
		for (T dest : destination) {
			resultSet.put(ySupplier.apply(dest), dest);
		}

		for (T sourceElement : source) {
			for (Range range : ranges) {
				int y = ySupplier.apply(sourceElement);
				if (range.contains(y - yOffset)) {
					resultSet.put(y, sourceElement);
					break;
				}
			}
		}

		@SuppressWarnings("unchecked")
		ListTag<T> resultList = (ListTag<T>) ListTag.createUnchecked(null);
		resultList.addAll(resultSet.values());
		return resultList;
	}

	default void mergeListTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name) {
		ListTag<ListTag<?>> sourceList = Helper.tagFromLevelFromRoot(source, name);
		ListTag<ListTag<?>> destinationList = Helper.tagFromLevelFromRoot(destination, name, sourceList);

		if (sourceList.size() != destinationList.size()) {
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

	default void mergeCompoundTagListsFromLevel(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<CompoundTag, Integer> ySupplier) {
		ListTag<CompoundTag> sourceElements = Helper.tagFromLevelFromRoot(source, name, new ListTag<>(CompoundTag.class));
		ListTag<CompoundTag> destinationElements = Helper.tagFromLevelFromRoot(destination, name, new ListTag<>(CompoundTag.class));

		initLevel(destination).put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
	}

	default void mergeCompoundTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<CompoundTag, Integer> ySupplier) {
		ListTag<CompoundTag> sourceElements = Helper.tagFromCompound(source, name, new ListTag<>(CompoundTag.class));
		ListTag<CompoundTag> destinationElements = Helper.tagFromCompound(destination, name, new ListTag<>(CompoundTag.class));

		destination.put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
	}

	// merge based on compound tag keys, assuming compound tag keys are ints
	default void mergeCompoundTags(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name) {
		CompoundTag sourceElements = Helper.tagFromCompound(source, name, new CompoundTag());
		CompoundTag destinationElements = Helper.tagFromCompound(destination, name, new CompoundTag());

		for (Map.Entry<String, Tag<?>> sourceElement : sourceElements) {
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
		ListTag<CompoundTag> entities = Helper.tagFromCompound(root, "Entities", null);
		if (entities != null) {
			entities.forEach(ChunkMerger::fixEntityUUID);
		}
	}

	private static void fixEntityUUID(CompoundTag entity) {
		Helper.fixEntityUUID(entity);
		if (entity.containsKey("Passengers")) {
			ListTag<CompoundTag> passengers = Helper.tagFromCompound(entity, "Passengers", null);
			if (passengers != null) {
				passengers.forEach(ChunkMerger::fixEntityUUID);
			}
		}
	}
}
