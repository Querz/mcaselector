package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.LongTag;

public class InhabitedTimeField extends Field<Long> {

	public InhabitedTimeField() {
		super(FieldType.INHABITED_TIME);
	}

	@Override
	public Long getOldValue(ChunkData data) {
		LongTag inhabitedTime = VersionHandler.getImpl(data, ChunkFilter.InhabitedTime.class).getInhabitedTime(data);
		return inhabitedTime == null ? null : inhabitedTime.asLong();
	}

	@Override
	public boolean parseNewValue(String s) {
		try {
			setNewValue(Long.parseLong(s));
			return true;
		} catch (NumberFormatException ex) {
			return super.parseNewValue(s);
		}
	}

	@Override
	public void change(ChunkData data) {
		ChunkFilter.InhabitedTime filter = VersionHandler.getImpl(data, ChunkFilter.InhabitedTime.class);
		LongTag tag = filter.getInhabitedTime(data);
		if (tag != null) {
			filter.setInhabitedTime(data, getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.InhabitedTime.class).setInhabitedTime(data, getNewValue());
	}
}
