package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point3i;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.NumberTag;

import java.util.Random;

public interface ChunkRelocator {

	boolean relocateChunk(CompoundTag root, Point3i offset);

	default boolean applyOffsetToSection(CompoundTag section, Point3i offset, int minY, int maxY) {
		NumberTag<?> value;
		if ((value = Helper.tagFromCompound(section, "Y")) != null) {
			if (value.asByte() > maxY || value.asByte() < minY) {
				return false;
			}

			int y = value.asByte() + offset.getY();
			if (y > maxY || y < minY) {
				return false;
			}
			section.putByte("Y", (byte) y);

			section.remove("BlockLight");
			section.remove("SkyLight");
		}
		return true;
	}
}
