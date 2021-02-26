package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
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
		Tag<?> rawSections = data.getRegion().getData().getCompoundTag("Level").get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return;
		}
		ListTag<CompoundTag> sections = ((ListTag<?>) rawSections).asCompoundTagList();
		for (int i = 0; i < sections.size(); i++) {
			CompoundTag section = sections.get(i);
			for (Range range : getNewValue()) {
				if (range.contains(section.getNumber("Y").intValue())) {
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
		return getType().toString() + " = \"" + sj.toString() + "\"";
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
