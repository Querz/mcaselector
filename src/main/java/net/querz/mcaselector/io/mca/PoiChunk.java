package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.PoiRelocator;
import net.querz.mcaselector.version.VersionController;
import java.io.File;

public class PoiChunk extends Chunk {

	public PoiChunk(Point2i absoluteLocation) {
		super(absoluteLocation);
	}

	@Override
	public boolean relocate(Point3i offset) {
		PoiRelocator relocator = VersionController.getPoiRelocator(data.getInt("DataVersion"));
		return relocator.relocatePoi(data, offset);
	}

	@Override
	public File getMCCFile() {
		return FileHelper.createPoiMCCFilePath(absoluteLocation);
	}
}
