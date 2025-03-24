package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.CompoundTag;
import java.io.File;
import java.util.List;

public class EntitiesChunk extends Chunk implements Cloneable {

	public EntitiesChunk(Point2i absoluteLocation) {
		super(absoluteLocation);
	}

	@Override
	public boolean relocate(Point3i offset) {
		return VersionHandler.getImpl(Helper.getDataVersion(data), ChunkFilter.RelocateEntities.class).relocate(data, offset);
	}

	@Override
	public void merge(CompoundTag destination, List<Range> ranges, int yOffset) {
		VersionHandler.getImpl(Helper.getDataVersion(data), ChunkFilter.MergeEntities.class).mergeChunks(data, destination, ranges, yOffset);
	}

	@Override
	public File getMCCFile() {
		return FileHelper.createEntitiesMCCFilePath(absoluteLocation);
	}

	@Override
	public EntitiesChunk clone() {
		return clone(EntitiesChunk::new);
	}
}
