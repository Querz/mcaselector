package net.querz.mcaselector.version.anvil114;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.PoiRelocator;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;

public class Anvil114PoiRelocator implements PoiRelocator {

	@Override
	public boolean relocatePoi(CompoundTag root, Point2i offset) {
		if (root == null || !root.containsKey("Sections")) {
			return false;
		}

		CompoundTag sections = catchClassCastException(() -> root.getCompoundTag("Sections"));
		if (sections == null) {
			return true;
		}

		for (Tag<?> s : sections.values()) {
			CompoundTag section = catchClassCastException(() -> (CompoundTag) s);
			if (section == null) {
				continue;
			}

			if (section.containsKey("Records") && section.get("Records").getID() != LongArrayTag.ID) {
				ListTag<CompoundTag> records = catchClassCastException(() -> section.getListTag("Records").asCompoundTagList());
				if (records == null) {
					continue;
				}

				for (CompoundTag record : records) {
					int[] pos = catchClassCastException(() -> record.getIntArray("pos"));
					applyOffsetToIntArrayPos(pos, offset);
				}
			}
		}
		return true;
	}

	private void applyOffsetToIntArrayPos(int[] pos, Point2i offset) {
		if (pos != null && pos.length == 3) {
			pos[0] += offset.getX();
			pos[2] += offset.getZ();
		}
	}
}
