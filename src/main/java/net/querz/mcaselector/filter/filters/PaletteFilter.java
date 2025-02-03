package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.BlockRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PaletteFilter extends TextFilter<List<String>> {

	private static final Comparator[] comparators = {
		Comparator.CONTAINS,
		Comparator.CONTAINS_NOT,
		Comparator.INTERSECTS,
		Comparator.EQUAL,
		Comparator.NOT_EQUAL
	};

	public PaletteFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private PaletteFilter(Operator operator, Comparator comparator, List<String> value) {
		super(FilterType.PALETTE, operator, comparator, value);
		setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public boolean matches(ChunkData data) {
		return switch (getComparator()) {
			case CONTAINS -> contains(value, data);
			case CONTAINS_NOT -> containsNot(value, data);
			case INTERSECTS -> intersects(value, data);
			case EQUAL -> equals(value, data);
			case NOT_EQUAL -> notEquals(value, data);
			default -> false;
		};
	}

	@Override
	public boolean contains(List<String> value, ChunkData data) {
		return VersionHandler.getImpl(data, ChunkFilter.Blocks.class).matchBlockNames(data, value);
	}

	@Override
	public boolean containsNot(List<String> value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(List<String> value, ChunkData data) {
		return VersionHandler.getImpl(data, ChunkFilter.Blocks.class).matchAnyBlockName(data, value);
	}

	public boolean equals(List<String> value, ChunkData data) {
		return VersionHandler.getImpl(data, ChunkFilter.Palette.class).paletteEquals(data, value);
	}

	public boolean notEquals(List<String> values, ChunkData data) {
		return !equals(values, data);
	}

	@Override
	public void setFilterValue(String raw) {
		String[] blockNames = BlockRegistry.parseBlockNames(raw);
		if (blockNames == null) {
			setValid(false);
			setValue(null);
		} else {
			setValid(true);
			setValue(Arrays.asList(blockNames));
			setRawValue(raw);
		}
	}

	@Override
	public String getFormatText() {
		return "<block>[,<block>,...]";
	}

	@Override
	public String toString() {
		return "Palette " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public PaletteFilter clone() {
		return new PaletteFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
