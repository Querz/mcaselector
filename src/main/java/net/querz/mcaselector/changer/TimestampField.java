package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;

public class TimestampField extends Field<Integer> {

	public TimestampField() {
		super(FieldType.TIMESTAMP);
	}

	@Override
	public Integer getOldValue(ChunkData root) {
		return root.getRegion().getTimestamp();
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
		if (root.getRegion() != null) {
			root.getRegion().setTimestamp(getNewValue());
		}
		if (root.getPoi() != null) {
			root.getPoi().setTimestamp(getNewValue());
		}
		if (root.getEntities() != null) {
			root.getEntities().setTimestamp(getNewValue());
		}
	}

	@Override
	public void force(ChunkData root) {
		change(root);
	}
}
