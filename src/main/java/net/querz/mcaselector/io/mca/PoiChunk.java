package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import java.io.File;
import java.util.List;

public class PoiChunk extends Chunk {

	public PoiChunk(Point2i absoluteLocation) {
		super(absoluteLocation);
	}

	@Override
	public boolean relocate(Point3i offset) {
		ChunkRelocator relocator = VersionController.getPoiRelocator(data.getInt("DataVersion"));
		return relocator.relocate(data, offset);
	}

	@Override
	public void merge(CompoundTag destination, List<Range> ranges, int yOffset) {
		ChunkMerger merger = VersionController.getPoiMerger(data.getInt("DataVersion"));
		merger.mergeChunks(data, destination, ranges, yOffset);
	}

	@Override
	public File getMCCFile() {
		return FileHelper.createPoiMCCFilePath(absoluteLocation);
	}

	@Override
	public PoiChunk clone() {
		return clone(PoiChunk::new);
	}
}
