package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;

public class RemoveLightingInfoField extends BooleanField {

	public RemoveLightingInfoField() {
		super(FieldType.REMOVE_LIGHTING_INFO);
	}

	@Override
	public void change(ChunkData data) {
		force(data);
	}

	@Override
	public void force(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.Light.class).removeLightingInfo(data);
	}
}
