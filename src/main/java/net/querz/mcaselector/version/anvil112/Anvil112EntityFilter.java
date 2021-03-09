package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

public class Anvil112EntityFilter implements EntityFilter {

	public void deleteEntities(ChunkData data) {
		Tag<?> rawEntities = data.getRegion().getData().getCompoundTag("Level").get("Entities");
		if (rawEntities == null || rawEntities.getID() == LongArrayTag.ID) {
			return;
		}
		((ListTag<?>) rawEntities).asCompoundTagList().clear();
	}

	@Override
	public ListTag<?> getEntities(ChunkData data) {
		Tag<?> rawEntities = data.getRegion().getData().getCompoundTag("Level").get("Entities");
		if (rawEntities == null || rawEntities.getID() != ListTag.ID) {
			return null;
		}
		return (ListTag<?>) rawEntities;
	}
}
