package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point2i;
import net.querz.nbt.tag.CompoundTag;

public interface PoiRelocator {

	boolean relocatePoi(CompoundTag root, Point2i offset);
}
