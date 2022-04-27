package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import net.querz.mcaselector.version.ChunkFilter;
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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.region().getData().getInt("DataVersion"));
		ListTag sections = chunkFilter.getSections(data.region().getData());
		if (sections == null) {
			return;
		}
		for (int i = 0; i < sections.size(); i++) {
			CompoundTag section = sections.getCompound(i);
			for (Range range : getNewValue()) {
				if (range.contains(section.getInt("Y"))) {
					sections.remove(i);
					i--;
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
