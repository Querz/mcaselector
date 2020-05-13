package net.querz.mcaselector.changer;

import net.querz.nbt.tag.CompoundTag;

public class DeleteSectionsField extends Field<Boolean> {

	public DeleteSectionsField() {
		super(FieldType.DELETE_SECTIONS);
	}

	@Override
	public Boolean getOldValue(CompoundTag root) {
		return false;
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
	public void change(CompoundTag root) {
		root.getCompoundTag("Level").getListTag("Sections").clear();
	}

	@Override
	public void force(CompoundTag root) {
		change(root);
	}
}
