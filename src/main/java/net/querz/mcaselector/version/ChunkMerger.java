package net.querz.mcaselector.version;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.CompoundTag;

import java.util.*;

public interface ChunkMerger {

	void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset);

	CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion);

}
