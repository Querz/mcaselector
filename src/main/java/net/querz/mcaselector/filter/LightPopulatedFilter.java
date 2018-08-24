package net.querz.mcaselector.filter;

public class LightPopulatedFilter extends ByteFilter {

	private static final Comparator[] comparators = {
			Comparator.EQ,
			Comparator.NEQ
	};

	public LightPopulatedFilter() {
		this(Operator.AND, Comparator.EQ, (byte) 0);
	}

	public LightPopulatedFilter(Operator operator, Comparator comparator, byte value) {
		super(FilterType.LIGHT_POPULATED, operator, comparator, value);
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	Byte getNumber(FilterData data) {
		return data.getChunk().getCompoundTag("Level").getByte("LightPopulated");
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (isValid() && (getFilterValue() != 1 && getFilterValue() != 0)) {
			setFilterNumber((byte) 0);
			setValid(false);
		}
	}

	@Override
	public String getFormatText() {
		return "1|0";
	}
}
