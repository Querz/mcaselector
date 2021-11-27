package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;

public abstract class TextFilter<T> extends Filter<T> {

	private static final Comparator[] comparators = {
			Comparator.CONTAINS,
			Comparator.CONTAINS_NOT,
			Comparator.INTERSECTS
	};

	protected T value;

	private Comparator comparator;

	public TextFilter(FilterType type, Operator operator, Comparator comparator, T value) {
		super(type, operator);
		this.comparator = comparator;
		this.value = value;
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public Comparator getComparator() {
		return comparator;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	@Override
	public boolean matches(ChunkData data) {
		return switch (comparator) {
			case CONTAINS -> contains(value, data);
			case CONTAINS_NOT -> containsNot(value, data);
			case INTERSECTS -> intersects(value, data);
			default -> false;
		};
	}

	@Override
	public T getFilterValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public abstract String getFormatText();

	public abstract boolean contains(T value, ChunkData data);

	public abstract boolean containsNot(T value, ChunkData data);

	public abstract boolean intersects(T value, ChunkData data);
}
