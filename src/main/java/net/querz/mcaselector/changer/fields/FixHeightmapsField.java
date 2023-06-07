package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.HeightmapCalculator;
import net.querz.mcaselector.version.VersionController;

public class FixHeightmapsField extends Field<Boolean> {

	public FixHeightmapsField() {
		super(FieldType.FIX_HEIGHTMAPS);
	}

	@Override
	public Boolean getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public boolean parseNewValue(String s) {
		if ("1".equals(s) || "true".equals(s)) {
			setNewValue(true);
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(ChunkData data) {
		HeightmapCalculator heightmapCalculator = VersionController.getHeightmapCalculator(data.region().getData().getIntOrDefault("DataVersion", 0));
		heightmapCalculator.worldSurface(data.region().getData());
		heightmapCalculator.oceanFloor(data.region().getData());
		heightmapCalculator.motionBlocking(data.region().getData());
		heightmapCalculator.motionBlockingNoLeaves(data.region().getData());
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
