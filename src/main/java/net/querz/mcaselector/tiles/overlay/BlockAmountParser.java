package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.version.VersionController;

public class BlockAmountParser extends OverlayParser {

	private static final int MIN_VALUE = 0;
	private static final int MAX_VALUE = 98304; // 384 * 16 * 16

	public BlockAmountParser() {
		super(OverlayType.BLOCK_AMOUNT);
		setMultiValues(new String[0]);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		return VersionController.getChunkFilter(chunkData.getRegion().getData().getInt("DataVersion")).getBlockAmount(chunkData.getRegion().getData(), multiValues());
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
				return setMin((Integer) null);
			}
			return setMin(value);
		} catch (NumberFormatException ex) {
			return setMin((Integer) null);
		}
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		try {
			int value = Integer.parseInt(raw);
			if (value < MIN_VALUE || value > MAX_VALUE) {
				return setMax((Integer) null);
			}
			return setMax(value);
		} catch (NumberFormatException ex) {
			return setMax((Integer) null);
		}
	}

	@Override
	public boolean setMultiValues(String raw) {
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
