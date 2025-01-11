package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.*;

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
		ChunkFilter.Heightmap heightmap = VersionHandler.getImpl(data, ChunkFilter.Heightmap.class);
		heightmap.worldSurface(data);
		heightmap.oceanFloor(data);
		heightmap.motionBlocking(data);
		heightmap.motionBlockingNoLeaves(data);
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
