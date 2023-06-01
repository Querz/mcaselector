package net.querz.mcaselector.version;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.ListTag;

import java.util.List;

public interface EntityFilter {

	void deleteEntities(ChunkData data, List<Range> ranges);

	ListTag getEntities(ChunkData data);
}
