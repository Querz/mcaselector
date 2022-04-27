package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.nbt.CompoundTag;
import java.util.List;

public class Anvil112EntityMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		// nothing to do until 1.17
	}

	@Override
	public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		// nothing to do until 1.17
		return null;
	}
}
