package net.querz.mcaselector.changer;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

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
		Tag<?> rawEntities = root.getCompoundTag("Level").get("Entities");
		if (rawEntities == null || rawEntities.getID() == LongArrayTag.ID) {
			return;
		}
		((ListTag<?>) rawEntities).asCompoundTagList().clear();
	}

	@Override
	public void force(CompoundTag root) {
		change(root);
	}
}
