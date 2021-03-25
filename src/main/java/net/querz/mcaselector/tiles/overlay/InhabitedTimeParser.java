package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.validation.ValidationHelper;

public class InhabitedTimeParser extends OverlayParser {

	private static final int MIN_VALUE = 0;
	private static final int MAX_VALUE = Integer.MAX_VALUE / 20;
	private String minDuration;
	private String maxDuration;

	public InhabitedTimeParser() {
		super(OverlayType.INHABITED_TIME);
	}

	public InhabitedTimeParser(int min, int max) {
		super(OverlayType.INHABITED_TIME);
		setMin(min);
		setMax(max);
		setActive(true);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		return ValidationHelper.withDefaultSilent(() -> chunkData.getRegion().getData().getCompoundTag("Level").getNumber("InhabitedTime").intValue(), 0);
	}

	@Override
	public String name() {
		return "InhabitedTime";
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
			return setMin((Integer) null);
		}
		try {
			return setMin(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			try {
				long duration = TextHelper.parseDuration(raw);
				if (duration > MAX_VALUE || duration < MIN_VALUE) {
					return setMin((Integer) null);
				}
				boolean res = setMin((int) (duration * 20));
				if (res) {
					minDuration = raw;
				}
				return res;
			} catch (IllegalArgumentException ex2) {
				return setMin((Integer) null);
			}
		}
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		maxDuration = null;
		if (raw == null || raw.isEmpty()) {
			return setMax((Integer) null);
		}
		try {
			return setMax(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			try {
				long duration = TextHelper.parseDuration(raw);
				if (duration > MAX_VALUE || duration < MIN_VALUE) {
					return setMax((Integer) null);
				}
				boolean res = setMax((int) (duration * 20));
				if (res) {
					maxDuration = raw;
				}
				return res;
			} catch (IllegalArgumentException ex2) {
				return setMax((Integer) null);
			}
		}
	}
}
