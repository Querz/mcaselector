package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.IntFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.ListTag;

public class EntityAmountFilter extends IntFilter {

	public EntityAmountFilter() {
		this(Operator.AND, Comparator.EQUAL, 0);
	}

	private EntityAmountFilter(Operator operator, Comparator comparator, int value) {
		super(FilterType.ENTITY_AMOUNT, operator, comparator, value);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		int dataVersion;
		if (data.region() != null && data.region().getData() != null) {
			dataVersion = data.region().getData().getInt("DataVersion");
		} else if (data.entities() != null && data.entities().getData() != null) {
			dataVersion = data.entities().getData().getInt("DataVersion");
		} else {
			return 0;
		}
		EntityFilter entityFilter = VersionController.getEntityFilter(dataVersion);
		ListTag entities = entityFilter.getEntities(data);
		if (entities == null) {
			return 0;
		}
		return entities.size();
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
