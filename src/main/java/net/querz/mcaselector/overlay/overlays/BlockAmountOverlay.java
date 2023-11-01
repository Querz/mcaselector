package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;

public class BlockAmountOverlay extends Overlay {

	private static final int MIN_VALUE = 0;
	private static final int MAX_VALUE = 98304; // 384 * 16 * 16

	public BlockAmountOverlay() {
		super(OverlayType.BLOCK_AMOUNT);
		setMultiValues(new String[0]);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		if (chunkData.region() == null || chunkData.region().getData() == null) {
			return 0;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(chunkData.region().getData().getIntOrDefault("DataVersion", 0));
		return chunkFilter.getBlockAmount(chunkData.region().getData(), multiValues());
	}

	@Override
	public String name() {
		return "Blocks";
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

	@Override
	public boolean setMultiValuesString(String raw) {
		if (raw == null) {
			setMultiValues(new String[0]);
			return false;
		}
		setRawMultiValues(raw);
		String[] blocks = TextHelper.parseBlockNames(raw);
		if (blocks == null) {
			setMultiValues(new String[0]);
			return false;
		} else {
			setMultiValues(blocks);
			return true;
		}
	}
}
