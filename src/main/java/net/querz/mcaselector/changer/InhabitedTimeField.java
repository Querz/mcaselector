package net.querz.mcaselector.changer;

import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.LongTag;

public class InhabitedTimeField extends Field<Long> {

	public InhabitedTimeField() {
		super(FieldType.INHABITED_TIME);
	}

	@Override
	public Long getOldValue(CompoundTag root) {
		return ValidationHelper.withDefault(() -> root.getCompoundTag("Level").getLong("InhabitedTime"), null);
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
		LongTag tag = root.getCompoundTag("Level").getLongTag("InhabitedTime");
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(CompoundTag root) {
		root.getCompoundTag("Level").putLong("InhabitedTime", getNewValue());
	}
}
