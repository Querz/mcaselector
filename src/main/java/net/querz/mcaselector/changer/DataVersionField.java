package net.querz.mcaselector.changer;

import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntTag;

public class DataVersionField extends Field<Integer> {

	public DataVersionField() {
		super(FieldType.DATA_VERSION);
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
