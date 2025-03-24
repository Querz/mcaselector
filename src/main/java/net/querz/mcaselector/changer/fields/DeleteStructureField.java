package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.StructureRegistry;
import net.querz.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class DeleteStructureField extends Field<List<String>> {

	public DeleteStructureField() {
		super(FieldType.DELETE_STRUCTURE);
	}

	@Override
	public List<String> getOldValue(ChunkData root) {
		return null;
	}

	@Override
	public boolean parseNewValue(String s) {
		if (getNewValue() != null) {
			getNewValue().clear();
		}

		List<String> value = new ArrayList<>();

		String[] structures = s.split(",");
		for (String structure : structures) {
			String trimmed = structure.trim();
			if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() > 2) {
				value.add(trimmed.substring(1, trimmed.length() - 1));
				continue;
			}
			if (StructureRegistry.isValidName(trimmed)) {
				value.add(trimmed);
				continue;
			}
			return super.parseNewValue(s);
		}
		if (value.isEmpty()) {
			return super.parseNewValue(s);
		}
		setNewValue(value);
		return true;
	}

	@Override
	public void change(ChunkData data) {
		ChunkFilter.Structures structures = VersionHandler.getImpl(data, ChunkFilter.Structures.class);
		CompoundTag references = structures.getStructureReferences(data);
		CompoundTag starts = structures.getStructureStarts(data);
		for (String structure : getNewValue()) {
			for (String alt : StructureRegistry.getAlts(structure)) {
				if (references != null) {
					references.remove(alt);
				}
				if (starts != null) {
					starts.remove(alt);
				}
			}
		}
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}

	@Override
	public String toString() {
		StringJoiner sj = new StringJoiner(", ");
		getNewValue().forEach(s -> sj.add(StructureRegistry.isValidName(s) ? s : "'" + s + "'"));
		return getType().toString() + " = \"" + sj + "\"";
	}

	@Override
	public String valueToString() {
		StringJoiner sj = new StringJoiner(", ");
		getNewValue().forEach(s -> sj.add(StructureRegistry.isValidName(s) ? s : "'" + s + "'"));
		return sj.toString();
	}
}
