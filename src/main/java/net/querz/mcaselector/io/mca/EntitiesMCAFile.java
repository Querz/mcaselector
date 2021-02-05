package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.CompressionType;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.tag.CompoundTag;
import java.io.File;
import java.util.List;
import java.util.Set;

public class EntitiesMCAFile extends MCAFile<EntitiesChunk> {

	public EntitiesMCAFile(File file) {
		super(file, EntitiesChunk::new);
		super.chunks = new EntitiesChunk[1024];
	}

	static EntitiesChunk newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		EntitiesChunk chunk = new EntitiesChunk(absoluteLocation);
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", dataVersion);
		chunk.data = root;
		chunk.compressionType = CompressionType.ZLIB;
		return chunk;
	}

	@Override
	public void mergeChunksInto(MCAFile<EntitiesChunk> destination, Point2i offset, boolean overwrite, Set<Point2i> sourceChunks, Set<Point2i> selection, List<Range> ranges) {
		mergeChunksInto(destination, offset, overwrite, sourceChunks, selection, ranges, EntitiesMCAFile::newEmptyChunk);
	}
}
