package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.*;

public class FixHeightmapsField extends BooleanField {

	public FixHeightmapsField() {
		super(FieldType.FIX_HEIGHTMAPS);
	}

	@Override
	public void change(ChunkData data) {
		force(data);
	}

	@Override
	public void force(ChunkData data) {
		ChunkFilter.Heightmap heightmap = VersionHandler.getImpl(data, ChunkFilter.Heightmap.class);
		heightmap.worldSurface(data);
		heightmap.oceanFloor(data);
		heightmap.motionBlocking(data);
		heightmap.motionBlockingNoLeaves(data);
	}
}
