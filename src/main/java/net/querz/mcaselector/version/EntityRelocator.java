package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point2i;
import net.querz.nbt.tag.CompoundTag;
import java.util.Random;

public interface EntityRelocator {

	Random random = new Random();

	boolean relocateEntities(CompoundTag root, Point2i offset);
}
