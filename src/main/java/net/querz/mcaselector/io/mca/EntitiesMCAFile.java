package net.querz.mcaselector.io.mca;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.tag.CompoundTag;
import java.io.File;
import java.util.List;

public class EntitiesMCAFile extends MCAFile<EntitiesChunk> implements Cloneable {

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
	public void mergeChunksInto(MCAFile<EntitiesChunk> destination, Point3i offset, boolean overwrite, LongOpenHashSet sourceChunks, LongOpenHashSet selection, List<Range> ranges) {
		mergeChunksInto(destination, offset, overwrite, sourceChunks, selection, ranges, EntitiesMCAFile::newEmptyChunk);
	}

	@Override
	public EntitiesMCAFile clone() {
		return clone(EntitiesMCAFile::new);
	}
}
