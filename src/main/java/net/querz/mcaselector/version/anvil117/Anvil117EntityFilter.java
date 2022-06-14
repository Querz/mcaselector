package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.ListTag;

public class Anvil117EntityFilter implements EntityFilter {

	public void deleteEntities(ChunkData data) {
		ListTag rawEntities = Helper.tagFromCompound(data.entities().getData(), "Entities", null);
		if (rawEntities != null) {
			rawEntities.clear();
		}
	}

	@Override
	public ListTag getEntities(ChunkData data) {
		if (data.entities() == null) {
			return null;
		}
		return Helper.tagFromCompound(data.entities().getData(), "Entities", null);
	}
}
