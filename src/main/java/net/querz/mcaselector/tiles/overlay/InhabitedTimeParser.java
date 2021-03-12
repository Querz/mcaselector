package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.validation.ValidationHelper;

public class InhabitedTimeParser extends OverlayDataParser {

	private static final int MAX_VALUE = 0;
	private static final int MIN_VALUE = Integer.MIN_VALUE / 20;

	public InhabitedTimeParser() {
		super(OverlayType.INHABITED_TIME);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		return ValidationHelper.withDefaultSilent(() -> chunkData.getRegion().getData().getCompoundTag("Level").getNumber("InhabitedTime").intValue(), 0);
	}

	@Override
	public String name() {
		return "InhabitedTime";
	}

	@Override
	public boolean setMin(String raw) {
		try {
			return setMin(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			try {
				long duration = TextHelper.parseDuration(raw);
				if (duration > MAX_VALUE || duration < MIN_VALUE) {
					return false;
				}
				return setMin((int) (duration * 20));
			} catch (IllegalArgumentException ex2) {
				return false;
			}
		}
	}

	@Override
	public boolean setMax(String raw) {
		try {
			return setMax(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			try {
				long duration = TextHelper.parseDuration(raw);
				if (duration > MAX_VALUE || duration < MIN_VALUE) {
					return false;
				}
				return setMax((int) (duration * 20));
			} catch (IllegalArgumentException ex2) {
				return false;
			}
		}
	}
}
