package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntTag;

public class DataVersionField extends Field<Integer> {

	public DataVersionField() {
		super(FieldType.DATA_VERSION);
	}

	@Override
	public Integer getOldValue(ChunkData data) {
		IntTag tag = Helper.getDataVersionTag(Helper.getRegion(data));
		return tag == null ? null : tag.asInt();
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
		CompoundTag root;
		if ((root = Helper.getRegion(data)) != null && Helper.getDataVersionTag(root) != null) {
			Helper.setDataVersion(root, getNewValue());
		}
		if ((root = Helper.getPOI(data)) != null && Helper.getDataVersionTag(root) != null) {
			Helper.setDataVersion(root, getNewValue());
		}
		if ((root = Helper.getEntities(data)) != null && Helper.getDataVersionTag(root) != null) {
			Helper.setDataVersion(root, getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		if (data.region() != null) {
			Helper.setDataVersion(data.region().getData(), getNewValue());
		}
		if (data.poi() != null) {
			Helper.setDataVersion(data.poi().getData(), getNewValue());
		}
		if (data.entities() != null) {
			Helper.setDataVersion(data.entities().getData(), getNewValue());
		}
	}
}
