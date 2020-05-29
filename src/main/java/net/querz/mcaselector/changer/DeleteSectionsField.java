package net.querz.mcaselector.changer;

import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteSectionsField extends Field<List<Range>> {

	private static final Pattern rangePattern = Pattern.compile("^(?<from>-?\\d*)(?<divider>:?)(?<to>-?\\d*)$");

	public DeleteSectionsField() {
		super(FieldType.DELETE_SECTIONS);
	}

	@Override
	public List<Range> getOldValue(CompoundTag root) {
		return null;
	}

	@Override
	public boolean parseNewValue(String s) {
		if (getNewValue() != null) {
			getNewValue().clear();
		}

		if ("true".equals(s.trim())) {
			if (getNewValue() == null) {
				setNewValue(new ArrayList<>());
			}
			getNewValue().add(new Range(Integer.MIN_VALUE, Integer.MAX_VALUE));
			return true;
		}

		// 1 --> single value range
		// :1 --> from negative infinity to 1
		// 1: --> from 1 to positive infinity
		// 1:4 --> from 1 to 4
		// : --> from negative infinity to positive infinity

		String[] split = s.split(",");
		for (String stringRange : split) {
			String trimmed = stringRange.trim();
			if (trimmed.isEmpty()) {
				return super.parseNewValue(s);
			}
			Matcher m = rangePattern.matcher(trimmed);
			if (m.find()) {
				String fromString = m.group("from");
				boolean divider = m.group("divider").isEmpty();
				String toString = m.group("to");

				int from = fromString.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(fromString);
				int to = toString.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(toString);

				if (getNewValue() == null) {
					setNewValue(new ArrayList<>());
				}

				if (divider) {
					getNewValue().add(new Range(from, from));
				} else {
					getNewValue().add(new Range(from, to));
				}
			} else {
				return super.parseNewValue(s);
			}
		}
		if (getNewValue() == null || getNewValue().size() != 0) {
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(CompoundTag root) {
		ListTag<CompoundTag> sections = root.getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
		for (int i = 0; i < sections.size(); i++) {
			CompoundTag section = sections.get(i);
			for (Range range : getNewValue()) {
				if (range.contains(section.getByte("Y"))) {
					sections.remove(i);
					i--;
				}
			}
		}
	}

	@Override
	public void force(CompoundTag root) {
		change(root);
	}

	@Override
	public String toString() {
		if (getNewValue().size() == 1 && getNewValue().get(0).isMaxRange()) {
			return getType().toString() + " = true";
		}
		StringJoiner sj = new StringJoiner(",");
		getNewValue().forEach(r -> sj.add(r.toString()));
		return getType().toString() + " = " + sj.toString();
	}
}
