package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.CompressionType;

public class CompressionFilter extends TextFilter<CompressionType> {

	private static final Comparator[] comparators = {
			Comparator.EQUAL,
			Comparator.NOT_EQUAL,
	};

	public CompressionFilter() {
		this(Operator.AND, Comparator.EQUAL, null, null);
	}

	private CompressionFilter(Operator operator, Comparator comparator, String rawValue, CompressionType value) {
		super(FilterType.COMPRESSION, operator, comparator, value);
		setRawValue(rawValue);
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public String getFormatText() {
		return "TYPE";
	}

	@Override
	public boolean matches(ChunkData data) {
		return switch (getComparator()) {
			case EQUAL -> isEqual(value, data);
			case NOT_EQUAL -> !isEqual(value, data);
			default -> false;
		};
	}

	public boolean isEqual(CompressionType value, ChunkData data) {
		return (data.region() != null || data.entities() != null || data.poi() != null)
				&& (data.region() == null || data.region().getCompressionType() == value)
				&& (data.entities() == null || data.entities().getCompressionType() == value)
				&& (data.poi() == null || data.poi().getCompressionType() == value);
	}

	@Override
	public boolean contains(CompressionType value, ChunkData data) {
		throw new UnsupportedOperationException("\"contains\" not allowed in CompressionFilter");
	}

	@Override
	public boolean containsNot(CompressionType value, ChunkData data) {
		throw new UnsupportedOperationException("\"!contains\" not allowed in CompressionFilter");
	}

	@Override
	public boolean intersects(CompressionType value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in CompressionFilter");
	}

	@Override
	public void setFilterValue(String raw) {
		for (CompressionType t : CompressionType.values()) {
			if (t.toString().equalsIgnoreCase(raw)) {
				setValue(t);
				setRawValue(raw);
				setValid(true);
				return;
			}
		}
		setValue(null);
		setValid(false);
	}

	@Override
	public String toString() {
		return "Compression " + getComparator().getQueryString() + " " + getFilterValue().toString();
	}

	@Override
	public Filter<CompressionType> clone() {
		return new CompressionFilter(getOperator(), getComparator(), getRawValue(), value);
	}
}
