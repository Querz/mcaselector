package net.querz.mcaselector.tiles.overlay;

import java.util.function.Supplier;

public enum OverlayType {

	ENTITY_AMOUNT("#Entities", EntityAmountParser::new),
	INHABITED_TIME("InhabitedTime", InhabitedTimeParser::new);

	private final String name;
	private final Supplier<OverlayParser> supplier;

	OverlayType(String name, Supplier<OverlayParser> supplier) {
		this.name = name;
		this.supplier = supplier;
	}

	public OverlayParser instance() {
		return supplier.get();
	}

	@Override
	public String toString() {
		return name;
	}
}
