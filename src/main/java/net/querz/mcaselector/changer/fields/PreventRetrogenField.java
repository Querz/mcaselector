package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;

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
		if (data.region() == null || data.region().getData() == null) {
			return;
		}
		data.region().getData().remove("below_zero_retrogen");
		data.region().getData().putString("Status", "full");
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
