package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.EntityRelocator;
import net.querz.nbt.tag.CompoundTag;

public class Anvil112EntityRelocator implements EntityRelocator {

	@Override
	public boolean relocateEntities(CompoundTag root, Point2i offset) {
		// nothing to do until 1.17
		return true;
	}
}
