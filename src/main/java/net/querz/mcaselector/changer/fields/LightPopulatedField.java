package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.ByteTag;

public class LightPopulatedField extends Field<Byte> {

	public LightPopulatedField() {
		super(FieldType.LIGHT_POPULATED);
	}

	@Override
	public Byte getOldValue(ChunkData data) {
		ChunkFilter.LightPopulated filter = VersionHandler.getImpl(data, ChunkFilter.LightPopulated.class);
		ByteTag lightPopulated = filter.getLightPopulated(data);
		return lightPopulated == null ? null : lightPopulated.asByte();
	}

	@Override
	public boolean parseNewValue(String s) {
		if ("1".equals(s)) {
			setNewValue((byte) 1);
			return true;
		} else if ("0".equals(s)) {
			setNewValue((byte) 0);
			return true;
		}
		return super.parseNewValue(s);

	}

	@Override
	public void change(ChunkData data) {
		ChunkFilter.LightPopulated filter = VersionHandler.getImpl(data, ChunkFilter.LightPopulated.class);
		ByteTag tag = filter.getLightPopulated(data);
		if (tag != null) {
			filter.setLightPopulated(data, getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.LightPopulated.class).setLightPopulated(data, getNewValue());
	}
}
