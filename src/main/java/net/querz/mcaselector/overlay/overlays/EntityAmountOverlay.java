package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountParser;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.ListTag;

public class EntityAmountOverlay extends AmountParser {

	public EntityAmountOverlay() {
		super(OverlayType.ENTITY_AMOUNT);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.region() == null || chunkData.region().getData() == null) {
			return 0;
		}
		EntityFilter entityFilter = VersionController.getEntityFilter(chunkData.getDataVersion());
		ListTag entities = entityFilter.getEntities(chunkData);
		return entities == null ? 0 : entities.size();
	}

	@Override
	public String name() {
		return "EntityAmount";
	}
}
