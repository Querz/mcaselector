package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.registry.StatusRegistry;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.StringTag;

public class StatusField extends Field<StatusRegistry.StatusIdentifier> {

	public StatusField() {
		super(FieldType.STATUS);
	}

	@Override
	public StatusRegistry.StatusIdentifier getOldValue(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.region().getData().getIntOrDefault("DataVersion", 0));
		StringTag status = chunkFilter.getStatus(data.region().getData());
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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.region().getData().getIntOrDefault("DataVersion", 0));
		StringTag tag = chunkFilter.getStatus(data.region().getData());
		if (tag != null) {
			chunkFilter.setStatus(data.region().getData(), getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.region().getData().getIntOrDefault("DataVersion", 0));
		chunkFilter.setStatus(data.region().getData(), getNewValue());
	}

	@Override
	public String valueToString() {
		return getNewValue().getStatusWithNamespace();
	}
}
