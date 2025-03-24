package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntTag;
import net.querz.nbt.LongArrayTag;
import net.querz.nbt.Tag;
import java.util.Map;

public class ReferenceField extends Field<Boolean> {

	// compoundtag: structure_name --> long_array
	// where each long is a x/z coordinate

	public ReferenceField() {
		super(FieldType.STRUCTURE_REFERENCE);
	}

	@Override
	public boolean parseNewValue(String s) {
		setNewValue(Boolean.parseBoolean(s));
		return true;
	}

	@Override
	public Boolean getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public void change(ChunkData data) {
		if (!getNewValue()) {
			return;
		}

		// attempt to fix chunk coordinates of structure references

		ChunkFilter.Structures filter = VersionHandler.getImpl(data, ChunkFilter.Structures.class);
		CompoundTag references = filter.getStructureReferences(data);
		if (references == null) {
			return;
		}
		ChunkFilter.Pos pos = VersionHandler.getImpl(data, ChunkFilter.Pos.class);
		IntTag xPos = pos.getXPos(data);
		IntTag zPos = pos.getZPos(data);
		if (xPos == null || zPos == null) {
			return;
		}
		for (Map.Entry<String, Tag> entry : references) {
			if (entry.getValue() instanceof LongArrayTag) {
				long[] structureReferences = ((LongArrayTag) entry.getValue()).getValue();

				for (int i = 0; i < structureReferences.length; i++) {
					int x = (int) (structureReferences[i]);
					int z = (int) (structureReferences[i] >> 32);
					if (Math.abs(x - xPos.asInt()) > 8 || Math.abs(z - zPos.asInt()) > 8) {
						structureReferences[i] = (zPos.asLong() & 0xFFFFFFFFL) << 32 | xPos.asLong() & 0xFFFFFFFFL;
					}
				}
			}
		}
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
