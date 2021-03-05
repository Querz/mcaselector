package net.querz.mcaselector.tiles;

import java.util.function.Supplier;

public enum OverlayDataParserType {

	INHABITED_TIME(InhabitedTimeParser::new);

	private Supplier<OverlayDataParser> supplier;

	OverlayDataParserType(Supplier<OverlayDataParser> supplier) {
		this.supplier = supplier;
	}

	public OverlayDataParser create() {
		return supplier.get();
	}
}
