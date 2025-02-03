package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.StringTag;

public class StatusFilter extends TextFilter<StatusRegistry.StatusIdentifier> {

	private static final Comparator[] comparators = {
			Comparator.EQUAL,
			Comparator.NOT_EQUAL,
	};

	public StatusFilter() {
		this(Operator.AND, Comparator.EQUAL, null, null);
	}

	private StatusFilter(Operator operator, Comparator comparator, String rawValue, StatusRegistry.StatusIdentifier value) {
		super(FilterType.STATUS, operator, comparator, value);
		setRawValue(rawValue);
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
		return switch (getComparator()) {
			case EQUAL -> isEqual(value, data);
			case NOT_EQUAL -> !isEqual(value, data);
			default -> false;
		};
	}

	public boolean isEqual(StatusRegistry.StatusIdentifier value, ChunkData data) {
		StringTag tag = VersionHandler.getImpl(data, ChunkFilter.Status.class).getStatus(data);
		return tag != null && value.equals(tag.getValue());
	}

	@Override
	public boolean contains(StatusRegistry.StatusIdentifier value, ChunkData data) {
		throw new UnsupportedOperationException("\"contains\" not allowed in StatusFilter");
	}

	@Override
	public boolean containsNot(StatusRegistry.StatusIdentifier value, ChunkData data) {
		throw new UnsupportedOperationException("\"!contains\" not allowed in StatusFilter");
	}

	@Override
	public boolean intersects(StatusRegistry.StatusIdentifier value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in StatusFilter");
	}

	@Override
	public void setFilterValue(String raw) {
		if (StatusRegistry.isValidName(raw)) {
			setValue(new StatusRegistry.StatusIdentifier(raw));
			setRawValue(raw);
			setValid(true);
		} else {
			setValue(null);
			setValid(false);
		}
	}

	@Override
	public String toString() {
		return "Status " + getComparator().getQueryString() + " " + getFilterValue().getStatusWithNamespace();
	}

	@Override
	public StatusFilter clone() {
		return new StatusFilter(getOperator(), getComparator(), getRawValue(), value);
	}
}
