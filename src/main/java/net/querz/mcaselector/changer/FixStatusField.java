package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;

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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		StringTag status = chunkFilter.getStatus(data.getRegion().getData());

		if ("empty".equals(status.getValue())) {
			ListTag<CompoundTag> sections = chunkFilter.getSections(data.getRegion().getData());
			if (sections == null) {
				return;
			}
			if (sections.size() > 0) {
				chunkFilter.setStatus(data.getRegion().getData(), "full");
			}
		}
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
