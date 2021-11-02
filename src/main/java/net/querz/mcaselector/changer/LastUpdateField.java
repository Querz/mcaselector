package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.tag.LongTag;

public class LastUpdateField extends Field<Long> {

	public LastUpdateField() {
		super(FieldType.LAST_UPDATE);
	}

	@Override
	public Long getOldValue(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		LongTag lastUpdate = chunkFilter.getLastUpdate(data.getRegion().getData());
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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		LongTag tag = chunkFilter.getLastUpdate(data.getRegion().getData());
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		chunkFilter.setLastUpdate(data.getRegion().getData(), getNewValue());
	}
}
