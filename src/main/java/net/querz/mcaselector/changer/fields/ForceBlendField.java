package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;

public class ForceBlendField extends BooleanField {

	public ForceBlendField() {
		super(FieldType.FORCE_BLEND);
	}

	@Override
	public void change(ChunkData data) {
		force(data);
	}

	@Override
	public void force(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.Blending.class).forceBlending(data);
	}
}
