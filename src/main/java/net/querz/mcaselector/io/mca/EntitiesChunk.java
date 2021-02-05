package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.EntityRelocator;
import net.querz.mcaselector.version.VersionController;

public class EntitiesChunk extends Chunk {

	public EntitiesChunk(Point2i absoluteLocation) {
		super(absoluteLocation);
	}

	public boolean relocate(Point2i offset) {
		EntityRelocator relocator = VersionController.getEntityRelocator(data.getInt("DataVersion"));
		return relocator.relocateEntities(data, offset);
	}
}
