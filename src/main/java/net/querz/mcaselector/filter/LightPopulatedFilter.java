package net.querz.mcaselector.filter;

public class LightPopulatedFilter extends ByteFilter {

	private static final Comparator[] comparators = {
			Comparator.EQUAL,
			Comparator.NOT_EQUAL
	};

	public LightPopulatedFilter() {
		this(Operator.AND, Comparator.EQUAL, (byte) 0);
	}

	private LightPopulatedFilter(Operator operator, Comparator comparator, byte value) {
		super(FilterType.LIGHT_POPULATED, operator, comparator, value);
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	Byte getNumber(FilterData data) {
		if (data.getRegion() == null) {
			return 0;
		}
		return data.getRegion().getData().getCompoundTag("Level").getByte("LightPopulated");
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

	@Override
	public LightPopulatedFilter clone() {
		return new LightPopulatedFilter(getOperator(), getComparator(), value);
	}
}
