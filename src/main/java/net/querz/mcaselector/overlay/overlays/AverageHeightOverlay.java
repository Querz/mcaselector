package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;

public class AverageHeightOverlay extends Overlay {

	private static final int MIN_VALUE = -64;
	private static final int MAX_VALUE = 320;

	public AverageHeightOverlay() {
		super(OverlayType.AVERAGE_HEIGHT);
	}

	@Override
	public int parseValue(ChunkData data) {
		return VersionHandler.getImpl(data, ChunkFilter.Blocks.class).getAverageHeight(data);
	}

	@Override
	public String name() {
		return "AverageHeight";
	}

	@Override
	public boolean setMin(String raw) {
		setRawMin(raw);
		try {
			int value = Integer.parseInt(raw);
			if (value < MIN_VALUE || value > MAX_VALUE) {
				return setMinInt(null);
			}
			return setMinInt(value);
		} catch (NumberFormatException ex) {
			return setMinInt(null);
		}
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		try {
			int value = Integer.parseInt(raw);
			if (value < MIN_VALUE || value > MAX_VALUE) {
				return setMaxInt(null);
			}
			return setMaxInt(value);
		} catch (NumberFormatException ex) {
			return setMaxInt(null);
		}
	}
}
