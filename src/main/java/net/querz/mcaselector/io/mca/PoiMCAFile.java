package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.CompoundTag;
import java.io.File;
import java.util.List;

public class PoiMCAFile extends MCAFile<PoiChunk> implements Cloneable {

	public PoiMCAFile(File file) {
		super(file, PoiChunk::new);
		super.chunks = new PoiChunk[1024];
	}

	static PoiChunk newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		CompoundTag root = VersionHandler.getImpl(dataVersion, ChunkFilter.MergePOI.class).newEmptyChunk(absoluteLocation, dataVersion);
		PoiChunk chunk = new PoiChunk(absoluteLocation);
		chunk.data = root;
		chunk.compressionType = CompressionType.ZLIB;
		return chunk;
	}

	@Override
	public void mergeChunksInto(MCAFile<PoiChunk> destination, Point3i offset, boolean overwrite, ChunkSet sourceChunks, ChunkSet selection, List<Range> ranges) {
		mergeChunksInto(destination, offset, overwrite, sourceChunks, selection, ranges, PoiMCAFile::newEmptyChunk);
	}

	@Override
	public PoiMCAFile clone() {
		return clone(PoiMCAFile::new);
	}
}
