package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.LongTag;

public class LastUpdateField extends Field<Long> {

	public LastUpdateField() {
		super(FieldType.LAST_UPDATE);
	}

	@Override
	public Long getOldValue(ChunkData data) {
		ChunkFilter.LastUpdate filter = VersionHandler.getImpl(data, ChunkFilter.LastUpdate.class);
		LongTag lastUpdate = filter.getLastUpdate(data);
		return lastUpdate == null ? null : lastUpdate.asLong();
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
		ChunkFilter.LastUpdate filter = VersionHandler.getImpl(data, ChunkFilter.LastUpdate.class);
		LongTag tag = filter.getLastUpdate(data);
		if (tag != null) {
			filter.setLastUpdate(data, getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.LastUpdate.class).setLastUpdate(data, getNewValue());
	}
}
