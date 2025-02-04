package net.querz.mcaselector.version.java_1_20;

import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_18.ChunkFilter_21w43a;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

public class ChunkFilter_24w09a {

	@MCVersionImplementation(3819)
	public static class Relocate extends ChunkFilter_21w43a.Relocate {

		@Override
		protected void applyOffsetToItem(CompoundTag item, Point3i offset) {
			if (item == null) {
				return;
			}

			CompoundTag components = Helper.tagFromCompound(item, "components");
			if (components == null) {
				return;
			}

			String id = Helper.stringFromCompound(item, "id", "");
			switch (id) {
			case "minecraft:compass":
				CompoundTag lodestoneTarget = Helper.tagFromCompound(components, "minecraft:lodestone_target");
				if (lodestoneTarget != null) {
					IntArrayTag pos = lodestoneTarget.getIntArrayTag("pos");
					if (pos != null) {
						Helper.applyOffsetToIntArrayPos(pos, offset);
					}
				}
				break;
			}

			// recursively update all items in child containers
			ListTag container = Helper.tagFromCompound(components, "minecraft:container");
			if (container != null && container.getElementType() == Tag.Type.COMPOUND) {
				for (CompoundTag i : container.iterateType(CompoundTag.class)) {
					if (i.contains("item", Tag.Type.COMPOUND)) {
						applyOffsetToItem(i.getCompoundTag("item"), offset);
					}
				}
			}
		}
	}
}
