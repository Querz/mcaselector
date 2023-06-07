package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.overlay.OverlayType;

public class TimestampOverlay extends Overlay {

	private String minTimestamp;
	private String maxTimestamp;

	public TimestampOverlay() {
		super(OverlayType.TIMESTAMP);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.region() == null) {
			return 0;
		}
		return chunkData.region().getTimestamp();
	}

	@Override
	public String name() {
		return "Timestamp";
	}

	public String minString() {
		return minTimestamp == null ? super.minString() : minTimestamp;
	}

	public String maxString() {
		return minTimestamp == null ? super.maxString() : maxTimestamp;
	}

	@Override
	public boolean setMin(String raw) {
		setRawMin(raw);
		minTimestamp = null;
		if (raw == null || raw.isEmpty()) {
			return setMinInt(null);
		}
		try {
			return setMinInt(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			try {
				int timestamp = TextHelper.parseTimestamp(raw);
				boolean res = setMinInt(timestamp);
				if (res) {
					minTimestamp = raw;
				}
				return res;
			} catch (IllegalArgumentException ex2) {
				return setMinInt(null);
			}
		}
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		maxTimestamp = null;
		if (raw == null || raw.isEmpty()) {
			return setMaxInt(null);
		}
		try {
			return setMaxInt(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			try {
				int timestamp = TextHelper.parseTimestamp(raw);
				boolean res = setMaxInt(timestamp);
				if (res) {
					maxTimestamp = raw;
				}
				return res;
			} catch (IllegalArgumentException ex2) {
				return setMaxInt(null);
			}
		}
	}
}
