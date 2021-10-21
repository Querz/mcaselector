package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point3i;
import net.querz.nbt.tag.CompoundTag;

public interface EntityRelocator {
	boolean relocateEntities(CompoundTag root, Point3i offset);
}
