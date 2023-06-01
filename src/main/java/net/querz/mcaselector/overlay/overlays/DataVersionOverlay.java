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
		return chunkData.getDataVersion();
	}

	@Override
	public String name() {
		return "DataVersion";
	}

	@Override
	public boolean setMin(String raw) {
		setRawMin(raw);
		try {
			return setMin(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return setMin((Integer) null);
		}
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		try {
			return setMax(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return setMax((Integer) null);
		}
	}
}
