package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.CompoundTag;

public class PreventRetrogenField extends BooleanField {

	public PreventRetrogenField() {
		super(FieldType.PREVENT_RETROGEN);
	}

	@Override
	public void change(ChunkData data) {
		force(data);
	}

	@Override
	public void force(ChunkData data) {
		CompoundTag root = Helper.getRegion(data);
		if (root == null) {
			return;
		}
		root.remove("below_zero_retrogen");
		root.putString("Status", "full");
	}
}
