package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.tag.IntTag;

public class DataVersionField extends Field<Integer> {

	public DataVersionField() {
		super(FieldType.DATA_VERSION);
	}

	@Override
	public Integer getOldValue(ChunkData data) {
		return ValidationHelper.withDefault(() -> data.getRegion().getData().getInt("DataVersion"), null);
	}

	@Override
	public boolean parseNewValue(String s) {
		try {
			if (s.matches("^[0-9]+$")) {
				setNewValue(Integer.parseInt(s));
				return true;
			}
		} catch (NumberFormatException ex) {
			// do nothing
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(ChunkData data) {
		IntTag tag = data.getRegion().getData().getIntTag("DataVersion");
		if (tag != null) {
			tag.setValue(getNewValue());
		}

		if (data.getPoi() != null) {
			tag = data.getPoi().getData().getIntTag("DataVersion");
			if (tag != null) {
				tag.setValue(getNewValue());
			}
		}

		if (data.getEntities() != null) {
			tag = data.getEntities().getData().getIntTag("DataVersion");
			if (tag != null) {
				tag.setValue(getNewValue());
			}
		}
	}

	@Override
	public void force(ChunkData data) {
		data.getRegion().getData().putInt("DataVersion", getNewValue());

		if (data.getPoi() != null) {
			data.getPoi().getData().putInt("DataVersion", getNewValue());
		}

		if (data.getEntities() != null) {
			data.getEntities().getData().putInt("DataVersion", getNewValue());
		}
	}
}
