package net.querz.mcaselector.overlay;

import net.querz.mcaselector.overlay.overlays.*;
import java.util.function.Supplier;

public enum OverlayType {

	INHABITED_TIME("InhabitedTime", InhabitedTimeOverlay::new),
	TIMESTAMP("Timestamp", TimestampOverlay::new),
	LAST_UPDATE("LastUpdate", LastUpdateOverlay::new),
	ENTITY_AMOUNT("#Entities", EntityAmountOverlay::new),
	TILE_ENTITY_AMOUNT("#TileEntities", TileEntityAmountOverlay::new),
	DATA_VERSION("DataVersion", DataVersionOverlay::new),
	AVERAGE_HEIGHT("AverageHeight", AverageHeightOverlay::new),
	BLOCK_AMOUNT("#Blocks", BlockAmountOverlay::new),
	CUSTOM("Custom", CustomOverlay::new);

	private final String name;
	private final Supplier<Overlay> supplier;

	OverlayType(String name, Supplier<Overlay> supplier) {
		this.name = name;
		this.supplier = supplier;
	}

	public Overlay instance() {
		return supplier.get();
	}

	@Override
	public String toString() {
		return name;
	}

	public static OverlayType getByName(String name) {
		for (OverlayType t : OverlayType.values()) {
			if (t.name.equals(name)) {
				return t;
			}
		}
		return null;
	}
}
