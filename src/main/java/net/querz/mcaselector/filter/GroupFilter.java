package net.querz.mcaselector.filter;

import net.querz.mcaselector.point.Point2i;
import java.util.ArrayList;
import java.util.List;

public class GroupFilter extends Filter<List<Filter<?>>> {

	private List<Filter<?>> children = new ArrayList<>();
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

	public int addFilter(Filter<?> filter) {
		filter.setParent(this);
		children.add(filter);
		return children.size() - 1;
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}

	//returns index of where this filter was added
	public int addFilterAfter(Filter<?> filter, Filter<?> after) {
		filter.setParent(this);
		int i = children.indexOf(after);
		if (i >= 0) {
			children.add(i + 1, filter);
		} else {
			children.add(filter);
		}
		return i + 1;
	}

	public boolean removeFilter(Filter<?> filter) {
		return children.remove(filter);
	}

	@Override
	public List<Filter<?>> getFilterValue() {
		return children;
	}

	@Override
	public void setFilterValue(String raw) {

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
	public void setComparator(Comparator comparator) {}

	@Override
	public boolean matches(FilterData data) {
		boolean currentResult = true;
		for (int i = 0; i < children.size(); i++) {
			//skip all condition in this AND block if it is already false
			if ((children.get(i).getOperator() == Operator.AND || i == 0) && currentResult) {
				currentResult = children.get(i).matches(data);
			} else if (children.get(i).getOperator() == Operator.OR) {
				//don't check other conditions if everything before OR is already true
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
		// if we have anything else than xPos and zPos filters, we apply to this region
		boolean currentResult = true;
		for (int i = 0; i < children.size(); i++) {
			Filter<?> child = children.get(i);
			if (child instanceof GroupFilter && ((GroupFilter) child).appliesToRegion(region)) {
				return true;
			} else if (child instanceof RegionMatcher) {
				RegionMatcher regionMatcher = (RegionMatcher) child;

				if ((child.getOperator() == Operator.AND || i == 0) && currentResult) {
					currentResult = regionMatcher.matchesRegion(region);
				} else if (child.getOperator() == Operator.OR) {
					//don't check other conditions if everything before OR is already true
					if (currentResult) {
						return true;
					}
					//otherwise, reset currentResult
					currentResult = regionMatcher.matchesRegion(region);
				}
			} else {
				return true;
			}
		}

		return currentResult;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	private String toString(int depth) {
		StringBuilder s = new StringBuilder(depth == 0 ? "" : "(");
		for (int i = 0; i < children.size(); i++) {
			s.append(i != 0 ? " " + children.get(i).getOperator() + " " : "");
			s.append(children.get(i).getType() == FilterType.GROUP ? ((GroupFilter) children.get(i)).toString(depth + 1) : children.get(i));
		}
		s.append(depth == 0 ? "" : ")");
		return s.toString();
	}

	@Override
	public boolean isValid() {
		for (Filter c : children) {
			if (!c.isValid()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public GroupFilter clone() {
		List<Filter<?>> cloneChildren = new ArrayList<>(children.size());
		children.forEach(c -> cloneChildren.add(c.clone()));
		GroupFilter clone = new GroupFilter(getOperator());
		clone.inverted = inverted;
		clone.children = cloneChildren;
		return clone;
	}
}
