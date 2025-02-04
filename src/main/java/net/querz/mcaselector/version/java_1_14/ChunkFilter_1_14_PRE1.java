package net.querz.mcaselector.version.java_1_14;

import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import java.util.List;
import java.util.Map;
import static net.querz.mcaselector.util.validation.ValidationHelper.silent;

public class ChunkFilter_1_14_PRE1 {

	@MCVersionImplementation(1947)
	public static class MergePOI implements ChunkFilter.MergePOI {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			mergeCompoundTags(source, destination, ranges, yOffset, "Sections");
		}

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			CompoundTag root = new CompoundTag();
			root.put("Sections", new CompoundTag());
			root.putInt("DataVersion", dataVersion);
			return root;
		}
	}

	@MCVersionImplementation(1947)
	public static class RelocatePOI implements ChunkFilter.RelocatePOI {

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
}
