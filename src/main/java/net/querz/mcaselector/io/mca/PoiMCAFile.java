package net.querz.mcaselector.io.mca;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import java.io.File;
import java.util.List;

public class PoiMCAFile extends MCAFile<PoiChunk> {

	public PoiMCAFile(File file) {
		super(file, PoiChunk::new);
		super.chunks = new PoiChunk[1024];
	}

	static PoiChunk newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		ChunkMerger chunkMerger = VersionController.getPoiMerger(dataVersion);
		CompoundTag root = chunkMerger.newEmptyChunk(absoluteLocation, dataVersion);
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
