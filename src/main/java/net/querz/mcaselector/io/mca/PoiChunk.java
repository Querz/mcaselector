package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.PoiRelocator;
import net.querz.mcaselector.version.VersionController;

public class PoiChunk extends Chunk {

	public PoiChunk(Point2i absoluteLocation) {
		super(absoluteLocation);
	}

	public boolean relocate(Point2i offset) {
		PoiRelocator relocator = VersionController.getPoiRelocator(data.getInt("DataVersion"));
		return relocator.relocatePoi(data, offset);
	}
}
