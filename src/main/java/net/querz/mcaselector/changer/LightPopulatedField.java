package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.tag.ByteTag;

public class LightPopulatedField extends Field<Byte> {

	public LightPopulatedField() {
		super(FieldType.LIGHT_POPULATED);
	}

	@Override
	public Byte getOldValue(ChunkData data) {
		return ValidationHelper.withDefault(() -> data.getRegion().getData().getCompoundTag("Level").getByte("LightPopulated"), null);
	}

	@Override
	public boolean parseNewValue(String s) {
		if ("1".equals(s)) {
			setNewValue((byte) 1);
			return true;
		} else if ("0".equals(s)) {
			setNewValue((byte) 0);
			return true;
		}
		return super.parseNewValue(s);

	}

	@Override
	public void change(ChunkData data) {
		ByteTag tag = data.getRegion().getData().getCompoundTag("Level").getByteTag("LightPopulated");
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		data.getRegion().getData().getCompoundTag("Level").putByte("LightPopulated", getNewValue());
	}
}
