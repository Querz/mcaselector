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

public class EntitiesMCAFile extends MCAFile<EntitiesChunk> implements Cloneable {

	public EntitiesMCAFile(File file) {
		super(file, EntitiesChunk::new);
		super.chunks = new EntitiesChunk[1024];
	}

	static EntitiesChunk newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		CompoundTag root = VersionHandler.getImpl(dataVersion, ChunkFilter.MergeEntities.class).newEmptyChunk(absoluteLocation, dataVersion);
		EntitiesChunk chunk = new EntitiesChunk(absoluteLocation);
		chunk.data = root;
		chunk.compressionType = CompressionType.ZLIB;
		return chunk;
	}

	@Override
	public void mergeChunksInto(MCAFile<EntitiesChunk> destination, Point3i offset, boolean overwrite, ChunkSet sourceChunks, ChunkSet targetChunks, List<Range> ranges) {
		mergeChunksInto(destination, offset, overwrite, sourceChunks, targetChunks, ranges, EntitiesMCAFile::newEmptyChunk);
	}

	@Override
	public EntitiesMCAFile clone() {
		return clone(EntitiesMCAFile::new);
	}
}
