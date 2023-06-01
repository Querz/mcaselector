package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.point.Point2i;
import java.util.ArrayList;
import java.util.List;

public class GroupFilter extends Filter<List<Filter<?>>> {

	private List<Filter<?>> children = new ArrayList<>();
	private boolean negated = false;

	public GroupFilter() {
		super(FilterType.GROUP);
	}

	public GroupFilter(boolean negated) {
		super(FilterType.GROUP);
		setNegated(negated);
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

	@Override
	public FilterType getType() {
		return negated ? FilterType.NOT_GROUP : FilterType.GROUP;
	}

	public void setNegated(boolean negated) {
		this.negated = negated;
	}

	public boolean isNegated() {
		return negated;
	}

	// returns index of where this filter was added
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

	public void removeFilter(Filter<?> filter) {
		children.remove(filter);
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
	public boolean matches(ChunkData data) {
		boolean currentResult = true;
		for (int i = 0; i < children.size(); i++) {
			// skip all condition in this AND block if it is already false
			if ((children.get(i).getOperator() == Operator.AND || i == 0) && currentResult) {
				currentResult = children.get(i).matches(data);
			} else if (children.get(i).getOperator() == Operator.OR) {
				// don't check other conditions if everything before OR is already true
				if (currentResult) {
					return !negated;
				}
				// otherwise, reset currentResult
				currentResult = children.get(i).matches(data);
			}
		}
		return negated != currentResult;
	}

	public boolean appliesToRegion(Point2i region) {
		GroupFilter gf = resolveNegations();

		// if we have anything else than xPos and zPos filters, we apply to this region
		boolean currentResult = true;
		for (int i = 0; i < gf.children.size(); i++) {
			Filter<?> child = gf.children.get(i);
			if (child instanceof GroupFilter && ((GroupFilter) child).appliesToRegion(region)) {
				currentResult = true;
			} else if (child instanceof RegionMatcher regionMatcher) {
				if ((child.getOperator() == Operator.AND || i == 0) && currentResult) {
					currentResult = regionMatcher.matchesRegion(region);
				} else if (child.getOperator() == Operator.OR) {
					// don't check other conditions if everything before OR is already true
					if (currentResult) {
						return true;
					}
					// otherwise, reset currentResult
					currentResult = regionMatcher.matchesRegion(region);
				}
			} else {
				return true;
			}
		}

		return currentResult;
	}

	private GroupFilter resolveNegations() {
		return resolveNegations(negated);
	}

	private GroupFilter resolveNegations(boolean negated) {
		GroupFilter group = new GroupFilter();
		GroupFilter workingGroup = new GroupFilter();
		for (int i = 0; i < children.size(); i++) {
			Filter<?> child = children.get(i);
			if (child instanceof GroupFilter) {
				group.addFilter(((GroupFilter) child).resolveNegations(negated));
				continue;
			}

			if (negated) {
				Filter<?> clone = child.clone();
				clone.setOperator(Operator.negate(child.getOperator()));
				clone.setComparator(Comparator.negate(child.getComparator()));

				if (clone.getOperator() == Operator.AND && i != 0) {
					if (workingGroup.children.size() == 1) {
						group.addFilter(workingGroup.children.get(0));
					} else {
						group.addFilter(workingGroup);
					}
					workingGroup = new GroupFilter();
				}
				workingGroup.addFilter(clone);
			} else {
				group.addFilter(child.clone());
			}
		}
		if (!workingGroup.isEmpty()) {
			if (workingGroup.children.size() == 1) {
				group.addFilter(workingGroup.children.get(0));
			} else {
				group.addFilter(workingGroup);
			}
		}
		return group;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	private String toString(int depth) {
		// add !(...) always
		// add (...) if depth is not 0
		// don't add empty groups

		boolean showGroup = (negated || depth != 0) && children.size() > 0;

		StringBuilder s = new StringBuilder(showGroup ? negated ? "!(" : "(" : "");
		for (int i = 0; i < children.size(); i++) {
			String child;
			if (children.get(i).getType() == FilterType.GROUP) {
				child = ((GroupFilter) children.get(i)).toString(depth + 1);
			} else {
				child = children.get(i).toString();
			}
			if (child.length() != 0) {
				s.append(i != 0 ? " " + children.get(i).getOperator() + " " : "").append(child);
			}
		}
		s.append(showGroup ? ")" : "");
		return s.toString();
	}

	@Override
	public boolean isValid() {
		for (Filter<?> c : children) {
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
		clone.negated = negated;
		clone.children = cloneChildren;
		return clone;
	}

	@Override
	public void resetTempData() {
		for (Filter<?> child : children) {
			child.resetTempData();
		}
	}

	@Override
	public boolean selectionOnly() {
		for (Filter<?> child : children) {
			if (child.selectionOnly()) {
				return true;
			}
		}
		return false;
	}
}
