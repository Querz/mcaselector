package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.tag.ListTag;

public class EntityAmountParser implements OverlayDataParser {

	@Override
	public long parseValue(ChunkData chunkData) {
		EntityFilter entityFilter = VersionController.getEntityFilter(chunkData.getRegion().getData().getInt("DataVersion"));
		ListTag<?> entities = entityFilter.getEntities(chunkData);
		if (entities == null) {
			return 0;
		}
		return entities.size();
	}

	@Override
	public String name() {
		return "EntityAmount";
	}
}
