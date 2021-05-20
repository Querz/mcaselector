package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;

public class DataVersionParser extends OverlayParser {

	public DataVersionParser() {
		super(OverlayType.DATA_VERSION);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		return ValidationHelper.withDefaultSilent(() -> chunkData.getRegion().getData().getInt("DataVersion"), 0);
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
