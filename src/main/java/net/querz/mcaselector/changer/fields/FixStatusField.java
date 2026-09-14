package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.ListTag;
import net.querz.nbt.StringTag;

public class FixStatusField extends BooleanField {

	public FixStatusField() {
		super(FieldType.FIX_STATUS);
	}

	private static final StatusRegistry.StatusIdentifier empty = new StatusRegistry.StatusIdentifier("empty");
	private static final StatusRegistry.StatusIdentifier full = new StatusRegistry.StatusIdentifier("full");

	@Override
	public void change(ChunkData data) {
		force(data);
	}

	@Override
	public void force(ChunkData data) {
		ChunkFilter.Status statusFilter = VersionHandler.getImpl(data, ChunkFilter.Status.class);
		StringTag status = statusFilter.getStatus(data);
		ChunkFilter.Sections sectionFilter = VersionHandler.getImpl(data, ChunkFilter.Sections.class);

		if (empty.equals(status.getValue())) {
			ListTag sections = sectionFilter.getSections(data);
			if (sections == null) {
				return;
			}
			if (!sections.isEmpty()) {
				statusFilter.setStatus(data, full);
			}
		}
	}
}
