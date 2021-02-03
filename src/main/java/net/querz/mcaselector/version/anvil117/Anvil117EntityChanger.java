package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.EntityChanger;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

public class Anvil117EntityChanger implements EntityChanger {

	public void deleteEntities(ChunkData data) {
		Tag<?> rawEntities = data.getEntities().getData().get("Entities");
		if (rawEntities == null || rawEntities.getID() == LongArrayTag.ID) {
			return;
		}
		((ListTag<?>) rawEntities).asCompoundTagList().clear();
	}
}
