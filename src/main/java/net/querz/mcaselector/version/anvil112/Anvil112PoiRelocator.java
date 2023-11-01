package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.nbt.CompoundTag;

public class Anvil112PoiRelocator implements ChunkRelocator {

	@Override
	public boolean relocate(CompoundTag root, Point3i offset) {
		// poi was introduced in 1.14, so we do nothing here
		return true;
	}
}
