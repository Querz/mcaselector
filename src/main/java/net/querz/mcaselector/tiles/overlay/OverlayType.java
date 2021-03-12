package net.querz.mcaselector.tiles.overlay;

import java.util.function.Supplier;

public enum OverlayType {

	INHABITED_TIME("InhabitedTime", InhabitedTimeParser::new),
	ENTITY_AMOUNT("#Entities", EntityAmountParser::new);

	private final String name;
	private final Supplier<OverlayDataParser> supplier;

	OverlayType(String name, Supplier<OverlayDataParser> supplier) {
		this.name = name;
		this.supplier = supplier;
	}

	public OverlayDataParser instance() {
		return supplier.get();
	}

	@Override
	public String toString() {
		return name;
	}
}
