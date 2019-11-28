package net.querz.mcaselector.filter;

import net.querz.mcaselector.point.Point2i;
import net.querz.nbt.CompoundTag;

public class EntityAmountFilter extends IntegerFilter {

	public EntityAmountFilter() {
		this(Operator.AND, Comparator.EQ, 0);
	}

	public EntityAmountFilter(Operator operator, Comparator comparator, int value) {
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
	public XPosFilter clone() {
		return new XPosFilter(getOperator(), getComparator(), value);
	}
}
