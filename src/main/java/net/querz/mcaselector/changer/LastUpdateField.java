package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.tag.LongTag;

public class LastUpdateField extends Field<Long> {

	public LastUpdateField() {
		super(FieldType.LAST_UPDATE);
	}

	@Override
	public Long getOldValue(ChunkData data) {
		return ValidationHelper.withDefault(() -> data.getRegion().getData().getCompoundTag("Level").getLong("LastUpdate"), null);
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
		LongTag tag = data.getRegion().getData().getCompoundTag("Level").getLongTag("LastUpdate");
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		data.getRegion().getData().getCompoundTag("Level").putLong("LastUpdate", getNewValue());
	}
}
