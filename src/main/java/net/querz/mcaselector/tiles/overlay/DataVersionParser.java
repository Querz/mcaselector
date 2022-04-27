package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;

public class DataVersionParser extends OverlayParser {

	public DataVersionParser() {
		super(OverlayType.DATA_VERSION);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.region() == null || chunkData.region().getData() == null) {
			return 0;
		}
		return chunkData.region().getData().getInt("DataVersion");
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
