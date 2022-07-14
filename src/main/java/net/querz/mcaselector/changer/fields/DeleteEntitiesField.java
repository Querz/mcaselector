package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.VersionController;

public class DeleteEntitiesField extends Field<Boolean> {

	public DeleteEntitiesField() {
		super(FieldType.DELETE_ENTITIES);
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
		VersionController.getEntityFilter(data.getDataVersion()).deleteEntities(data, null);
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
