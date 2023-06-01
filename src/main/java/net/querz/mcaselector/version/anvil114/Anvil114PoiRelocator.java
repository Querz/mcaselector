package net.querz.mcaselector.version.anvil114;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.silent;

public class Anvil114PoiRelocator implements ChunkRelocator {

	@Override
	public boolean relocate(CompoundTag root, Point3i offset) {
		CompoundTag sections = Helper.tagFromCompound(root, "Sections");
		if (sections == null) {
			return false;
		}

		CompoundTag newSections = new CompoundTag();

		for (Map.Entry<String, Tag> s : sections) {
			CompoundTag section = silent(() -> (CompoundTag) s.getValue(), null);
			if (section == null) {
				continue;
			}

			if (section.containsKey("Records") && section.get("Records").getType() != Tag.Type.LONG_ARRAY) {
				ListTag records = Helper.tagFromCompound(section, "Records");
				if (records != null) {
					for (CompoundTag record : records.iterateType(CompoundTag.class)) {
						int[] pos = Helper.intArrayFromCompound(record, "pos");
						Helper.applyOffsetToIntArrayPos(pos, offset);
					}
				}
			}

			if (s.getKey().matches("^[0-9]{1,2}$")) {
				int y = Integer.parseInt(s.getKey()) + offset.getY();
				if (y >= 0 && y <= 15) {
					newSections.put("" + y, section);
				}
			}
		}

		root.put("Sections", newSections);

		return true;
	}
}
