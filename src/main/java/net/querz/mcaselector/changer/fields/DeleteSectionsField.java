package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import java.util.List;
import java.util.StringJoiner;

public class DeleteSectionsField extends Field<List<Range>> {

	public DeleteSectionsField() {
		super(FieldType.DELETE_SECTIONS);
	}

	@Override
	public List<Range> getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public boolean parseNewValue(String s) {
		if (getNewValue() != null) {
			getNewValue().clear();
		}

		setNewValue(RangeParser.parseRanges(s, ","));
		if (getNewValue() != null && getNewValue().size() != 0) {
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(ChunkData data) {
		if (data.region() != null && data.region().getData() != null) {
			ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
			chunkFilter.deleteSections(data.region().getData(), getNewValue());
		}
		// delete entities and poi as well
		if (data.entities() != null && data.entities().getData() != null) {
			EntityFilter entityFilter = VersionController.getEntityFilter(data.getDataVersion());
			entityFilter.deleteEntities(data, getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}

	@Override
	public String toString() {
		if (getNewValue().size() == 1 && getNewValue().get(0).isMaxRange()) {
			return getType().toString() + " = true";
		}
		StringJoiner sj = new StringJoiner(", ");
		getNewValue().forEach(r -> sj.add(r.toString()));
		return getType().toString() + " = \"" + sj + "\"";
	}

	@Override
	public String valueToString() {
		if (getNewValue().size() == 1 && getNewValue().get(0).isMaxRange()) {
			return "true";
		}
		StringJoiner sj = new StringJoiner(", ");
		getNewValue().forEach(r -> sj.add(r.toString()));
		return sj.toString();
	}
}
