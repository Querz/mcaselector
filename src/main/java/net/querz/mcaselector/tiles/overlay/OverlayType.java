package net.querz.mcaselector.tiles.overlay;

public enum OverlayType {

	INHABITED_TIME(new InhabitedTimeParser()),
	ENTITY_AMOUNT(new EntityAmountParser());

	private final OverlayDataParser instance;

	OverlayType(OverlayDataParser instance) {
		this.instance = instance;
	}

	public OverlayDataParser instance() {
		return instance;
	}
}
