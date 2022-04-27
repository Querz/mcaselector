package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.ListTag;

public class EntityAmountParser extends AmountParser {

	public EntityAmountParser() {
		super(OverlayType.ENTITY_AMOUNT);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.region() == null || chunkData.region().getData() == null) {
			return 0;
		}
		EntityFilter entityFilter = VersionController.getEntityFilter(chunkData.region().getData().getInt("DataVersion"));
		ListTag entities = entityFilter.getEntities(chunkData);
		return entities == null ? 0 : entities.size();
	}

	@Override
	public String name() {
		return "EntityAmount";
	}
}
