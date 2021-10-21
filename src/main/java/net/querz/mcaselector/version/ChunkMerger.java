package net.querz.mcaselector.version;

import net.querz.mcaselector.range.Range;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public interface ChunkMerger {

	void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset);

	default <T extends Tag<?>> ListTag<T> mergeLists(ListTag<T> source, ListTag<T> destination, List<Range> ranges, Function<T, Integer> ySupplier, int yOffset) {
		@SuppressWarnings("unchecked")
		ListTag<T> resultList = (ListTag<T>) ListTag.createUnchecked(null);

		Set<T> resultSet = new HashSet<>();
		for (T dest : destination) {
			resultSet.add(dest);
		}

		elem: for (T destinationElement : destination) {
			for (Range range : ranges) {
				if (range.contains(ySupplier.apply(destinationElement))) {
					resultSet.remove(destinationElement);
					continue elem;
				}
			}
		}

		elem: for (T sourceElement : source) {
			for (Range range : ranges) {
				if (range.contains(ySupplier.apply(sourceElement) - yOffset)) {
					resultSet.add(sourceElement);
					continue elem;
				}
			}
		}

		resultList.addAll(resultSet);
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

	default void mergeCompoundTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<CompoundTag, Integer> ySupplier) {
		ListTag<CompoundTag> sourceElements = Helper.tagFromLevelFromRoot(source, name, new ListTag<>(CompoundTag.class));
		ListTag<CompoundTag> destinationElements = Helper.tagFromLevelFromRoot(destination, name, new ListTag<>(CompoundTag.class));

		initLevel(destination).put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
	}

	default CompoundTag initLevel(CompoundTag c) {
		CompoundTag level = Helper.levelFromRoot(c);
		if (level == null) {
			c.put("Level", level = new CompoundTag());
		}
		return level;
	}

	default void fixEntityUUIDs(CompoundTag root) {
		ListTag<CompoundTag> entities = Helper.tagFromLevelFromRoot(root, "Entities", null);
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
