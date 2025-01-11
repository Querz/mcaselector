package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.CompoundTag;

public class PreventRetrogenField extends Field<Boolean> {

	public PreventRetrogenField() {
		super(FieldType.PREVENT_RETROGEN);
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
		CompoundTag root = Helper.getRegion(data);
		if (root == null) {
			return;
		}
		root.remove("below_zero_retrogen");
		root.putString("Status", "full");
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
