package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountParser;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.ListTag;

public class EntityAmountOverlay extends AmountParser {

	public EntityAmountOverlay() {
		super(OverlayType.ENTITY_AMOUNT);
	}

	@Override
	public int parseValue(ChunkData data) {
		ListTag entities = VersionHandler.getImpl(data, ChunkFilter.Entities.class).getEntities(data);
		return entities == null ? 0 : entities.size();
	}

	@Override
	public String name() {
		return "EntityAmount";
	}
}
