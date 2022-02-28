package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.nbt.tag.CompoundTag;

public class ForceBlendField extends Field<Boolean> {

	public ForceBlendField() {
		super(FieldType.FORCE_BLEND);
	}

	@Override
	public Boolean getOldValue(ChunkData root) {
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
	public void change(ChunkData root) {
		// blending_data
		CompoundTag blendingData = new CompoundTag();
		blendingData.putByte("old_noise", (byte) 1);
		root.getRegion().getData().put("blending_data", blendingData);

		// delete Heightmaps
		root.getRegion().getData().remove("Heightmaps");

		// remove isLightOn
		root.getRegion().getData().remove("isLightOn");
	}

	@Override
	public void force(ChunkData root) {
		change(root);
	}
}
