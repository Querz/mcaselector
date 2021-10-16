package net.querz.mcaselector.io.mca;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.tag.CompoundTag;
import java.io.File;
import java.util.List;
import java.util.Set;

public class PoiMCAFile extends MCAFile<PoiChunk> {

	public PoiMCAFile(File file) {
		super(file, PoiChunk::new);
		super.chunks = new PoiChunk[1024];
	}

	static PoiChunk newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		PoiChunk chunk = new PoiChunk(absoluteLocation);
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", dataVersion);
		chunk.data = root;
		chunk.compressionType = CompressionType.ZLIB;
		return chunk;
	}

	@Override
	public void mergeChunksInto(MCAFile<PoiChunk> destination, Point2i offset, boolean overwrite, LongOpenHashSet sourceChunks, LongOpenHashSet selection, List<Range> ranges) {
		mergeChunksInto(destination, offset, overwrite, sourceChunks, selection, ranges, PoiMCAFile::newEmptyChunk);
	}
}
