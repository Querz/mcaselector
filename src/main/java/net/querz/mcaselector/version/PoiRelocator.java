package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point3i;
import net.querz.nbt.tag.CompoundTag;

public interface PoiRelocator {

	boolean relocatePoi(CompoundTag root, Point3i offset);
}
