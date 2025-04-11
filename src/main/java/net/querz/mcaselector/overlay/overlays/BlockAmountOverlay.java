package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountOverlay;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.BlockRegistry;

public class BlockAmountOverlay extends AmountOverlay {

	public BlockAmountOverlay() {
		super(OverlayType.BLOCK_AMOUNT, 0, Integer.MAX_VALUE);
		setMultiValues(new String[0]);
	}

	@Override
	public int parseValue(ChunkData data) {
		return VersionHandler.getImpl(data, ChunkFilter.Blocks.class).getBlockAmount(data, multiValues());
	}

	@Override
	public String name() {
		return "Blocks";
	}

	@Override
	public boolean setMultiValuesString(String raw) {
		if (raw == null) {
			setMultiValues(new String[0]);
			return false;
		}
		setRawMultiValues(raw);
		String[] blocks = BlockRegistry.parseBlockNames(raw);
		if (blocks == null) {
			setMultiValues(new String[0]);
			return false;
		} else {
			setMultiValues(blocks);
			return true;
		}
	}
}
