package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;

public class TimestampField extends Field<Integer> {

	public TimestampField() {
		super(FieldType.TIMESTAMP);
	}

	@Override
	public Integer getOldValue(ChunkData data) {
		return data.region().getTimestamp();
	}

	@Override
	public boolean parseNewValue(String s) {
		try {
			setNewValue(Integer.parseInt(s));
			return true;
		} catch (NumberFormatException ex) {
			return super.parseNewValue(s);
		}
	}

	@Override
	public void change(ChunkData data) {
		if (data.region() != null) {
			data.region().setTimestamp(getNewValue());
		}
		if (data.poi() != null) {
			data.poi().setTimestamp(getNewValue());
		}
		if (data.entities() != null) {
			data.entities().setTimestamp(getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
