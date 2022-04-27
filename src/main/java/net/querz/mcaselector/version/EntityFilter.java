package net.querz.mcaselector.version;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.nbt.ListTag;

public interface EntityFilter {

	void deleteEntities(ChunkData data);

	ListTag getEntities(ChunkData data);
}
