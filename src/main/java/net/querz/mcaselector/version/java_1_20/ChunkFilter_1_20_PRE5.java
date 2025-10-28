package net.querz.mcaselector.version.java_1_20;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_18.ChunkFilter_21w43a;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.StringTag;
import java.util.List;

public class ChunkFilter_1_20_PRE5 {

	@MCVersionImplementation(3458)
	public static class Sections extends ChunkFilter_21w43a.Sections {

		@Override
		public void deleteSections(ChunkData data, List<Range> ranges) {
			switch (Helper.getRegion(data).getString("Status")) {
			case "minecraft:light", "minecraft:spawn", "minecraft:heightmaps", "minecraft:full" -> Helper.getRegion(data).putString("Status", "minecraft:features");
			default -> {return;}
			}
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return;
			}
			for (int i = 0; i < sections.size(); i++) {
				CompoundTag section = sections.getCompound(i);
				for (Range range : ranges) {
					if (range.contains(section.getInt("Y"))) {
						deleteSection(section);
					}
				}
			}
		}
	}

	@MCVersionImplementation(3458)
	public static class Merge extends ChunkFilter_21w43a.Merge {

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			CompoundTag root = new CompoundTag();
			root.putInt("xPos", absoluteLocation.getX());
			root.putInt("yPos", -4);
			root.putInt("zPos", absoluteLocation.getZ());
			root.putString("Status", "minecraft:full");
			root.putInt("DataVersion", dataVersion);
			return root;
		}
	}

	@MCVersionImplementation(3458)
	public static class Status extends ChunkFilter_21w43a.Status {

		@Override
		public void setStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			if (Helper.getRegion(data) != null) {
				Helper.getRegion(data).putString("Status", status.getStatusWithNamespace());
			}
		}

		@Override
		public boolean matchStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			StringTag tag = getStatus(data);
			if (tag == null) {
				return false;
			}
			return status.getStatusWithNamespace().equals(tag.getValue());
		}
	}
}
