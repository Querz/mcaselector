package net.querz.mcaselector.filter;

import net.querz.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;

public class ConditionGroup implements Filter {

	private List<Filter> filters = new ArrayList<>();
	private Operator operator = Operator.AND;

	public ConditionGroup() {}

	public ConditionGroup(Operator operator) {
		this.operator = operator;
	}

	public void addFilter(Filter filter) {
		filters.add(filter);
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

	@Override
	public boolean matches(FilterData data) {
		boolean currentResult = true;
		for (int i = 0; i < filters.size(); i++) {
			//skip all condition in this AND block if it is already false
			if ((filters.get(i).getOperator() == Operator.AND || i == 0) && currentResult) {
				currentResult = filters.get(i).matches(data);
			} else if (filters.get(i).getOperator() == Operator.OR) {
				//don't check other conditions if everything before OR is  already true
				if (currentResult) {
					return true;
				}
				//otherwise, reset currentResult
				currentResult = filters.get(i).matches(data);
			}
		}
		return currentResult;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("(");
		for (int i = 0; i < filters.size(); i++) {
			if (i != 0) {
				s.append(filters.get(i).getOperator() == Operator.AND ? " && " : " || ");
			}
			s.append(filters.get(i).toString());
		}
		return s + ")";
	}
}
