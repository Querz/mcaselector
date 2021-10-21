package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.EntityRelocator;
import net.querz.mcaselector.version.VersionController;
import java.io.File;

public class EntitiesChunk extends Chunk {

	public EntitiesChunk(Point2i absoluteLocation) {
		super(absoluteLocation);
	}

	@Override
	public boolean relocate(Point3i offset) {
		EntityRelocator relocator = VersionController.getEntityRelocator(data.getInt("DataVersion"));
		return relocator.relocateEntities(data, offset);
	}

	@Override
	public File getMCCFile() {
		return FileHelper.createEntitiesMCCFilePath(absoluteLocation);
	}
}
