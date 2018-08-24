package net.querz.mcaselector.filter;

import net.querz.nbt.StringTag;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StatusFilter extends TextFilter<String> {

	private static final Set<String> validStatus = new HashSet<>();
	private static final Comparator[] comparators = {
			Comparator.EQ,
			Comparator.NEQ
	};

	static {
		validStatus.addAll(Arrays.asList("empty", "base", "carved", "liquid_carved", "decorated", "lighted", "mobs_spawned", "finalized", "fullchunk", "postprocessed"));
	}

	public StatusFilter() {
		this(Operator.AND, Comparator.EQ, null);
	}

	public StatusFilter(Operator operator, Comparator comparator, String value) {
		super(FilterType.STATUS, operator, comparator, value);
		setRawValue(value);
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public String getFormatText() {
		return "empty,base,...";
	}

	@Override
	public boolean contains(String value, FilterData data) {
		StringTag tag = data.getChunk().getCompoundTag("Level").getStringTag("Status");
		return tag != null && validStatus.contains(tag.getValue());
	}

	@Override
	public boolean containsNot(String value, FilterData data) {
		StringTag tag = data.getChunk().getCompoundTag("Level").getStringTag("Status");
		return tag == null || !validStatus.contains(tag.getValue());
	}

	@Override
	public void setFilterValue(String raw) {
		if (validStatus.contains(raw)) {
			setValue(raw);
			setValid(true);
		} else {
			setValue(null);
			setValid(false);
		}
	}

	@Override
	public String toString(FilterData data) {
		StringTag tag = data.getChunk().getCompoundTag("Level").getStringTag("Status");
		return getFilterValue() + " " + getComparator() + " " + (tag == null ? "null" : tag.getValue());
	}

	@Override
	public String toString() {
		return "Status " + getComparator() + " " + (getFilterValue() != null ? getFilterValue() : "null");
	}
}
