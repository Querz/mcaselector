package net.querz.mcaselector.filter;

public class EntityAmountFilter extends IntFilter {

	public EntityAmountFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	private EntityAmountFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.ENTITY_AMOUNT, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(FilterData data) {
		return data.getChunk().getCompoundTag("Level").getListTag("Entities").size();
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (isValid() && getFilterValue() < 0) {
			setFilterNumber(0);
			setValid(false);
		}
	}

	@Override
	public EntityAmountFilter clone() {
		return new EntityAmountFilter(getOperator(), getComparator(), value);
	}
}
