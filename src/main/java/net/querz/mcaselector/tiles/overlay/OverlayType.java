package net.querz.mcaselector.tiles.overlay;

public enum OverlayType {

	INHABITED_TIME(new InhabitedTimeParser());

	private final OverlayDataParser instance;

	OverlayType(OverlayDataParser instance) {
		this.instance = instance;
	}

	public OverlayDataParser instance() {
		return instance;
	}
}
