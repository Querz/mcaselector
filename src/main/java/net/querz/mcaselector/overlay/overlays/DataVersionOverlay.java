package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.overlay.OverlayType;

public class DataVersionOverlay extends Overlay {

	public DataVersionOverlay() {
		super(OverlayType.DATA_VERSION);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.region() == null || chunkData.region().getData() == null) {
			return 0;
		}
		return chunkData.region().getData().getIntOrDefault("DataVersion", 0);
	}

	@Override
	public String name() {
		return "DataVersion";
	}

	@Override
	public boolean setMin(String raw) {
		setRawMin(raw);
		try {
			return setMinInt(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return setMinInt(null);
		}
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		try {
			return setMaxInt(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return setMaxInt(null);
		}
	}
}
