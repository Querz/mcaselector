package net.querz.mcaselector.changer;

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
		if (data.getRegion() == null || data.getRegion().getData() == null) {
			return;
		}
		data.getRegion().getData().remove("below_zero_retrogen");
		data.getRegion().getData().putString("Status", "full");
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
