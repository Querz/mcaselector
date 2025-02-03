package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.StringTag;

public class StatusField extends Field<StatusRegistry.StatusIdentifier> {

	public StatusField() {
		super(FieldType.STATUS);
	}

	@Override
	public StatusRegistry.StatusIdentifier getOldValue(ChunkData data) {
		ChunkFilter.Status filter = VersionHandler.getImpl(data, ChunkFilter.Status.class);
		StringTag status = filter.getStatus(data);
		return status == null ? null : new StatusRegistry.StatusIdentifier(status.getValue(), true);
	}

	@Override
	public boolean parseNewValue(String s) {
		if (StatusRegistry.isValidName(s)) {
			setNewValue(new StatusRegistry.StatusIdentifier(s));
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(ChunkData data) {
		ChunkFilter.Status filter = VersionHandler.getImpl(data, ChunkFilter.Status.class);
		StringTag tag = filter.getStatus(data);
		if (tag != null) {
			filter.setStatus(data, getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.Status.class).setStatus(data, getNewValue());
	}

	@Override
	public String valueToString() {
		return getNewValue().getStatusWithNamespace();
	}
}
