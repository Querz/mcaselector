package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

public class Anvil117EntityFilter implements EntityFilter {

	public void deleteEntities(ChunkData data) {
		if (data.getEntities() == null || data.getEntities().getData() == null) {
			return;
		}
		Tag<?> rawEntities = data.getEntities().getData().get("Entities");
		if (rawEntities == null || rawEntities.getID() == LongArrayTag.ID) {
			return;
		}
		((ListTag<?>) rawEntities).asCompoundTagList().clear();
	}

	@Override
	public ListTag<?> getEntities(ChunkData data) {
		if (data.getEntities() == null || data.getEntities().getData() == null) {
			return null;
		}
		Tag<?> rawEntities = data.getEntities().getData().get("Entities");
		if (rawEntities == null || rawEntities.getID() != ListTag.ID) {
			return null;
		}
		return (ListTag<?>) rawEntities;
	}
}
