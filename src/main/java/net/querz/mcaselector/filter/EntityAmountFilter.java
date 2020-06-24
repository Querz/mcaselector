package net.querz.mcaselector.filter;

import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

public class EntityAmountFilter extends IntFilter {

	public EntityAmountFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	private EntityAmountFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.ENTITY_AMOUNT, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(FilterData data) {
		Tag<?> rawEntities = data.getChunk().getCompoundTag("Level").get("Entities");
		if (rawEntities == null || rawEntities.getID() == LongArrayTag.ID) {
			return 0;
		}
		return ((ListTag<?>) rawEntities).asCompoundTagList().size();
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
