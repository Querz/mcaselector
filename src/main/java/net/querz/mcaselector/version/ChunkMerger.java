package net.querz.mcaselector.version;

import net.querz.mcaselector.range.Range;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public interface ChunkMerger {

	Random random = new Random();

	void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges);

	default <T extends Tag<?>> ListTag<T> mergeLists(ListTag<T> source, ListTag<T> destination, List<Range> ranges, Function<T, Integer> ySupplier) {
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
				if (range.contains(ySupplier.apply(sourceElement))) {
					resultSet.add(sourceElement);
					continue elem;
				}
			}
		}

		resultList.addAll(resultSet);
		return resultList;
	}
}
