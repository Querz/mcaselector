package net.querz.mcaselector.changer;

import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.IntTag;

public class DataVersionField extends Field<Integer> {

	public DataVersionField() {
		super(FieldType.DATA_VERSION);
	}

	@Override
	public Integer getOldValue(CompoundTag root) {
		return ValidationHelper.withDefault(() -> root.getInt("DataVersion"), null);
	}

	@Override
	public boolean parseNewValue(String s) {
		try {
			if (s.matches("^[0-9]+$")) {
				setNewValue(Integer.parseInt(s));
				return true;
			}
		} catch (NumberFormatException ex) {
			//do nothing
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(CompoundTag root) {
		IntTag tag = root.getIntTag("DataVersion");
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(CompoundTag root) {
		root.putInt("DataVersion", getNewValue());
	}
}
