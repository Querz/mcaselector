package net.querz.mcaselector.changer;

import net.querz.nbt.tag.CompoundTag;

public class DeleteEntitiesField extends Field<Boolean> {

	public DeleteEntitiesField() {
		super(FieldType.DELETE_ENTITIES);
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
		root.getCompoundTag("Level").getListTag("Entities").clear();
	}

	@Override
	public void force(CompoundTag root) {
		change(root);
	}
}
