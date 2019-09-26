package net.querz.mcaselector.changer;

import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.LongTag;

public class LastUpdateField extends Field<Long> {

	public LastUpdateField() {
		super(FieldType.LAST_UPDATE);
	}

	@Override
	public Long getOldValue(CompoundTag root) {
		return ValidationHelper.withDefault(() -> root.getCompoundTag("Level").getLong("LastUpdate"), null);
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
	public void change(CompoundTag root) {
		LongTag tag = root.getCompoundTag("Level").getLongTag("LastUpdate");
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(CompoundTag root) {
		root.getCompoundTag("Level").putLong("LastUpdate", getNewValue());
	}
}
