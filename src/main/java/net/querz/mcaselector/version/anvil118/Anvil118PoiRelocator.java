package net.querz.mcaselector.version.anvil118;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.PoiRelocator;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;

public class Anvil118PoiRelocator implements PoiRelocator {

	@Override
	public boolean relocatePoi(CompoundTag root, Point3i offset) {
		if (root == null || !root.containsKey("Sections")) {
			return false;
		}

		CompoundTag sections = catchClassCastException(() -> root.getCompoundTag("Sections"));
		if (sections == null) {
			return true;
		}

		CompoundTag newSections = new CompoundTag();

		for (Map.Entry<String, Tag<?>> s : sections) {
			CompoundTag section = catchClassCastException(() -> (CompoundTag) s.getValue());
			if (section == null) {
				continue;
			}

			if (section.containsKey("Records") && section.get("Records").getID() != LongArrayTag.ID) {
				ListTag<CompoundTag> records = catchClassCastException(() -> section.getListTag("Records").asCompoundTagList());
				if (records != null) {
					for (CompoundTag record : records) {
						int[] pos = catchClassCastException(() -> record.getIntArray("pos"));
						Helper.applyOffsetToIntArrayPos(pos, offset);
					}
				}
			}

			if (s.getKey().matches("^-?[0-9]{1,2}$")) {
				int y = Integer.parseInt(s.getKey()) + offset.getY();
				if (y >= -4 && y <= 19) {
					newSections.put("" + y, section);
				}
			}
		}

		root.put("Sections", newSections);

		return true;
	}
}
