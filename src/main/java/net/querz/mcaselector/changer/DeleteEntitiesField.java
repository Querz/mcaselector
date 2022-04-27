package net.querz.mcaselector.changer;

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
		VersionController.getEntityFilter(data.region().getData().getInt("DataVersion")).deleteEntities(data);
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
