package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.ListTag;
import net.querz.nbt.StringTag;

public class FixStatusField extends Field<Boolean> {

	public FixStatusField() {
		super(FieldType.FIX_STATUS);
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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		StringTag status = chunkFilter.getStatus(data.region().getData());

		if ("empty".equals(status.getValue())) {
			ListTag sections = chunkFilter.getSections(data.region().getData());
			if (sections == null) {
				return;
			}
			if (sections.size() > 0) {
				chunkFilter.setStatus(data.region().getData(), "full");
			}
		}
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
