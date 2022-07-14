package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;

public class ForceBlendField extends Field<Boolean> {

	public ForceBlendField() {
		super(FieldType.FORCE_BLEND);
	}

	@Override
	public Boolean getOldValue(ChunkData root) {
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
	public void change(ChunkData root) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(root.getDataVersion());
		chunkFilter.forceBlending(root.region().getData());
	}

	@Override
	public void force(ChunkData root) {
		change(root);
	}
}
