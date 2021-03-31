package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

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
		CompoundTag level = ValidationHelper.withDefault(() -> data.getRegion().getData().getCompoundTag("Level"), null);
		if (level == null) {
			return;
		}

		if ("empty".equals(level.getString("Status"))) {
			ListTag<CompoundTag> sections = ValidationHelper.withDefault(() -> level.getListTag("Sections").asCompoundTagList(), null);
			if (sections == null) {
				return;
			}
			if (sections.size() > 0) {
				level.putString("Status", "full");
			}
		}
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
