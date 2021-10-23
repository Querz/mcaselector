package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.VersionController;
import java.io.File;

public class RegionChunk extends Chunk implements Cloneable {

	public RegionChunk(Point2i absoluteLocation) {
		super(absoluteLocation);
	}

	@Override
	public boolean relocate(Point3i offset) {
		ChunkRelocator relocator = VersionController.getChunkRelocator(data.getInt("DataVersion"));
		return relocator.relocateChunk(data, offset);
	}

	@Override
	public File getMCCFile() {
		return FileHelper.createRegionMCCFilePath(absoluteLocation);
	}

	@Override
	public RegionChunk clone() {
		return clone(RegionChunk::new);
	}
}
