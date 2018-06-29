package net.querz.mcaselector.filter.structure;

import net.querz.mcaselector.util.Point2i;
import java.util.ArrayList;
import java.util.List;

public class GroupFilter extends Filter<List<Filter>> {

	private List<Filter> children = new ArrayList<>();
	private boolean inverted = false;

	public GroupFilter() {
		super(FilterType.GROUP);
	}

	public GroupFilter(Operator operator) {
		super(FilterType.GROUP, operator);
	}

	public boolean isEmpty() {
		return children.isEmpty();
	}

	public int addFilter(Filter filter) {
		filter.setParent(this);
		children.add(filter);
		return children.size() - 1;
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}

	//returns index of where this filter was added
	public int addFilterAfter(Filter filter, Filter after) {
		filter.setParent(this);
		int i = children.indexOf(after);
		if (i >= 0) {
			children.add(i + 1, filter);
		} else {
			children.add(filter);
		}
		return i + 1;
	}

	public boolean removeFilter(Filter filter) {
		return children.remove(filter);
	}

	@Override
	public List<Filter> getFilterValue() {
		return children;
	}

	@Override
	public boolean setFilterValue(String raw) {
		return false;
	}

	@Override
	public Comparator[] getComparators() {
		return new Comparator[0];
	}

	@Override
	public Comparator getComparator() {
		return null;
	}

	@Override
	public boolean matches(FilterData data) {
		boolean currentResult = true;
		for (int i = 0; i < children.size(); i++) {
			//skip all condition in this AND block if it is already false
			if ((children.get(i).getOperator() == Operator.AND || i == 0) && currentResult) {
				currentResult = children.get(i).matches(data);
			} else if (children.get(i).getOperator() == Operator.OR) {
				//don't check other conditions if everything before OR is  already true
				if (currentResult) {
					return !inverted;
				}
				//otherwise, reset currentResult
				currentResult = children.get(i).matches(data);
			}
		}
		return inverted != currentResult;
	}

	public boolean appliesToRegion(Point2i region) {
		for (Filter child : children) {
			if (child.getOperator() == Operator.OR) {
				return true;
			}
		}

		for (Filter child : children) {
			if (child instanceof XPosFilter && !((XPosFilter) child).matchesRegion(region)
				|| child instanceof ZPosFilter && !((ZPosFilter) child).matchesRegion(region)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString(FilterData data) {
		StringBuilder s = new StringBuilder("(");
		for (int i = 0; i < children.size(); i++) {
			s.append(i != 0 ? " " + children.get(i).getOperator() + " " : "");
			s.append(children.get(i).toString(data));
		}
		s.append(")");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("(");
		for (int i = 0; i < children.size(); i++) {
			s.append(i != 0 ? " " + children.get(i).getOperator() + " " : "");
			s.append(children.get(i));
		}
		s.append(")");
		return s.toString();
	}
}
