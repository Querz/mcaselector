package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;

public class TimestampField extends Field<Integer> {

	public TimestampField() {
		super(FieldType.TIMESTAMP);
	}

	@Override
	public Integer getOldValue(ChunkData root) {
		return root.region().getTimestamp();
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
	public void change(ChunkData root) {
		if (root.region() != null) {
			root.region().setTimestamp(getNewValue());
		}
		if (root.poi() != null) {
			root.poi().setTimestamp(getNewValue());
		}
		if (root.entities() != null) {
			root.entities().setTimestamp(getNewValue());
		}
	}

	@Override
	public void force(ChunkData root) {
		change(root);
	}
}
