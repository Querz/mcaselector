package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.LongTag;

public class InhabitedTimeField extends Field<Long> {

	public InhabitedTimeField() {
		super(FieldType.INHABITED_TIME);
	}

	@Override
	public Long getOldValue(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		LongTag inhabitedTime = chunkFilter.getInhabitedTime(data.region().getData());
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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		LongTag tag = chunkFilter.getInhabitedTime(data.region().getData());
		if (tag != null) {
			chunkFilter.setInhabitedTime(data.region().getData(), getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		chunkFilter.setInhabitedTime(data.region().getData(), getNewValue());
	}
}
