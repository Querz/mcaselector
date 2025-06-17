package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountOverlay;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.CompoundTag;

public class DataVersionOverlay extends AmountOverlay {

	public DataVersionOverlay() {
		super(OverlayType.DATA_VERSION, 0, Integer.MAX_VALUE);
	}

	@Override
	public int parseValue(ChunkData data) {
		CompoundTag root = Helper.getRegion(data);
		if (root == null) {
			return 0;
		}
		return root.getIntOrDefault("DataVersion", 0);
	}

	@Override
	public String name() {
		return "DataVersion";
	}
}
