package net.querz.mcaselector.overlay;

import net.querz.mcaselector.overlay.parsers.*;
import net.querz.mcaselector.tile.overlay.parsers.*;

import java.util.function.Supplier;

public enum OverlayType {

	INHABITED_TIME("InhabitedTime", InhabitedTimeParser::new),
	TIMESTAMP("Timestamp", TimestampParser::new),
	LAST_UPDATE("LastUpdate", LastUpdateParser::new),
	ENTITY_AMOUNT("#Entities", EntityAmountParser::new),
	TILE_ENTITY_AMOUNT("#TileEntities", TileEntityAmountParser::new),
	DATA_VERSION("DataVersion", DataVersionParser::new),
	AVERAGE_HEIGHT("AverageHeight", AverageHeightParser::new),
	BLOCK_AMOUNT("#Blocks", BlockAmountParser::new),
	CUSTOM("Custom", CustomParser::new);

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
