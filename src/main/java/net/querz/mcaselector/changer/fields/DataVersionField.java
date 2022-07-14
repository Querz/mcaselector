package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.IntTag;

public class DataVersionField extends Field<Integer> {

	public DataVersionField() {
		super(FieldType.DATA_VERSION);
	}

	@Override
	public Integer getOldValue(ChunkData data) {
		return ValidationHelper.withDefault(data::getDataVersion, null);
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
		IntTag tag = data.region().getData().getIntTag("DataVersion");
		if (tag != null) {
			data.region().getData().putInt("DataVersion", getNewValue());
		}

		if (data.poi() != null) {
			tag = data.poi().getData().getIntTag("DataVersion");
			if (tag != null) {
				data.region().getData().putInt("DataVersion", getNewValue());
			}
		}

		if (data.entities() != null) {
			tag = data.entities().getData().getIntTag("DataVersion");
			if (tag != null) {
				data.region().getData().putInt("DataVersion", getNewValue());
			}
		}
	}

	@Override
	public void force(ChunkData data) {
		data.region().getData().putInt("DataVersion", getNewValue());

		if (data.poi() != null) {
			data.poi().getData().putInt("DataVersion", getNewValue());
		}

		if (data.entities() != null) {
			data.entities().getData().putInt("DataVersion", getNewValue());
		}
	}
}
