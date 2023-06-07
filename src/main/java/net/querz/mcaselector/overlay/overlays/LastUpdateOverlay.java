package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.LongTag;

public class LastUpdateOverlay extends Overlay {

	private static final int MIN_VALUE = 0;
	private static final int MAX_VALUE = Integer.MAX_VALUE / 20;
	private String minDuration;
	private String maxDuration;

	public LastUpdateOverlay() {
		super(OverlayType.LAST_UPDATE);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(chunkData.region().getData().getIntOrDefault("DataVersion", 0));
		LongTag tag = chunkFilter.getLastUpdate(chunkData.region().getData());
		return tag == null ? 0 : tag.asInt();
	}

	@Override
	public String name() {
		return "LastUpdate";
	}

	public String minString() {
		return minDuration == null ? super.minString() : minDuration;
	}

	public String maxString() {
		return maxDuration == null ? super.maxString() : maxDuration;
	}

	@Override
	public boolean setMin(String raw) {
		setRawMin(raw);
		minDuration = null;
		if (raw == null || raw.isEmpty()) {
			return setMinInt(null);
		}
		try {
			return setMinInt(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			try {
				long duration = TextHelper.parseDuration(raw);
				if (duration > MAX_VALUE || duration < MIN_VALUE) {
					return setMinInt(null);
				}
				boolean res = setMinInt((int) (duration * 20));
				if (res) {
					minDuration = raw;
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
		maxDuration = null;
		if (raw == null || raw.isEmpty()) {
			return setMaxInt(null);
		}
		try {
			return setMaxInt(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			try {
				long duration = TextHelper.parseDuration(raw);
				if (duration > MAX_VALUE || duration < MIN_VALUE) {
					return setMaxInt(null);
				}
				boolean res = setMaxInt((int) (duration * 20));
				if (res) {
					maxDuration = raw;
				}
				return res;
			} catch (IllegalArgumentException ex2) {
				return setMaxInt(null);
			}
		}
	}
}
