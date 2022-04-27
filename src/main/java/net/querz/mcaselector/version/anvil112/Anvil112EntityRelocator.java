package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.nbt.CompoundTag;

public class Anvil112EntityRelocator implements ChunkRelocator {

	@Override
	public boolean relocate(CompoundTag root, Point3i offset) {
		// nothing to do until 1.17
		return true;
	}
}
