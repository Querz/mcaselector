package net.querz.mcaselector.filter;

import net.querz.mcaselector.debug.Debug;
import net.querz.nbt.tag.StringTag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StatusFilter extends TextFilter<String> {

	private static final Set<String> validStatus = new HashSet<>();
	private static final Comparator[] comparators = {
			Comparator.EQUAL,
			Comparator.NOT_EQUAL
	};

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/all_status.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validStatus.add(line);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_status.txt for StatusFilter", ex);
		}
	}

	public StatusFilter() {
		this(Operator.AND, Comparator.EQUAL, null);
	}

	private StatusFilter(Operator operator, Comparator comparator, String value) {
		super(FilterType.STATUS, operator, comparator, value);
		setRawValue(value);
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public String getFormatText() {
		return "empty,...";
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
	public String toString() {
		return "Status " + getComparator().getQueryString() + " " + getFilterValue();
	}

	@Override
	public StatusFilter clone() {
		return new StatusFilter(getOperator(), getComparator(), value);
	}
}
