package net.querz.mcaselector.filter;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.ChunkData;
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
			Comparator.NOT_EQUAL,
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
	public boolean matches(ChunkData data) {
		switch (getComparator()) {
			case EQUAL:
				return isEqual(value, data);
			case NOT_EQUAL:
				return !isEqual(value, data);
		}
		return false;
	}

	public boolean isEqual(String value, ChunkData data) {
		if (data.getRegion() == null) {
			return false;
		}
		StringTag tag = data.getRegion().getData().getCompoundTag("Level").getStringTag("Status");
		return tag != null && value.equals(tag.getValue());
	}

	@Override
	public boolean contains(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"contains\" not allowed in StatusFilter");
	}

	@Override
	public boolean containsNot(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"!contains\" not allowed in StatusFilter");
	}

	@Override
	public boolean intersects(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in StatusFilter");
	}

	@Override
	public void setFilterValue(String raw) {
		if (validStatus.contains(raw)) {
			setValue(raw);
			setRawValue(raw);
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
