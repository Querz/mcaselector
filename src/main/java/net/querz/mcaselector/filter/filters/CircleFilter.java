package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.IntTag;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CircleFilter extends TextFilter<List<CircleFilter.CircleFilterDefinition>> implements RegionMatcher {

	private static final Comparator[] comparators = {
			Comparator.CONTAINS,
			Comparator.CONTAINS_NOT
	};

	public CircleFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	CircleFilter(Operator operator, Comparator comparator, List<CircleFilterDefinition> value) {
		super(FilterType.CIRCLE, operator, comparator, value);
		setRawValue(value == null ? "" : value.stream().map(CircleFilterDefinition::toString).collect(Collectors.joining(",")));
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public void setFilterValue(String raw) {
		if (raw == null) {
			setValid(false);
			setValue(null);
			return;
		}

		List<CircleFilterDefinition> centers = new ArrayList<>();
		// format: <x>;<z>;<r>[,<x>;<z>;<r>...]

		String[] split = raw.split(",");
		for (String c : split) {
			String[] rawCoordinate = c.split(";");
			if (rawCoordinate.length != 3) {
				setValid(false);
				setValue(null);
				return;
			}
			Integer x = TextHelper.parseInt(rawCoordinate[0], 10);
			Integer z = TextHelper.parseInt(rawCoordinate[1], 10);
			Integer r = TextHelper.parseInt(rawCoordinate[2], 10);

			if (x == null || z == null || r == null || r < 0) {
				setValid(false);
				setValue(null);
				return;
			}

			centers.add(new CircleFilterDefinition(new Point2i(x, z), r));
		}

		setValid(true);
		setValue(centers);
		setRawValue(raw);
	}

	@Override
	public Filter<List<CircleFilterDefinition>> clone() {
		return new CircleFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}

	@Override
	public String getFormatText() {
		return "<x>;<z>;<r>[,<x>;<z>;<r>...]";
	}

	@Override
	public boolean contains(List<CircleFilterDefinition> value, ChunkData data) {
		if (data.region() == null || data.region().getData() == null) {
			return false;
		}

		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		IntTag xPos = chunkFilter.getXPos(data.region().getData());
		IntTag zPos = chunkFilter.getZPos(data.region().getData());
		if (xPos == null || zPos == null) {
			return false;
		}

		Point2i chunk = new Point2i(xPos.asInt(), zPos.asInt());
		for (CircleFilterDefinition circle : value) {
			if (circle.matches(chunk)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsNot(List<CircleFilterDefinition> value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(List<CircleFilterDefinition> value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in circle filter");
	}

	@Override
	public boolean matchesRegion(Point2i region) {
		Point2i topLeft = region.regionToChunk();
		Point2i bottomLeft = topLeft.add(0, 31);
		Point2i bottomRight = topLeft.add(31, 31);
		Point2i topRight = topLeft.add(31, 0);

		switch (getComparator()) {
		case CONTAINS:
			for (CircleFilterDefinition circle : value) {
				// check if center is actually in this region
				if (circle.center.chunkToRegion().equals(region)) {
					return true;
				}

				// check distance of center to region corners
				if (circle.matches(topLeft) || circle.matches(bottomLeft) || circle.matches(bottomRight) || circle.matches(topRight)) {
					return true;
				}

				// check if circle overlaps with region edges
				Point2i topMost = circle.center.sub(0, circle.radius);
				Point2i rightMost = circle.center.add(circle.radius, 0);
				Point2i bottomMost = circle.center.add(0, circle.radius);
				Point2i leftMost = circle.center.sub(circle.radius, 0);
				if (circle.center.chunkToRegion().getX() == region.getX()) {
					if (circle.center.getZ() > bottomLeft.getZ() && topMost.getZ() <= bottomLeft.getZ()) {
						return true;
					}
					if (circle.center.getZ() < topRight.getZ() && bottomMost.getZ() >= topRight.getZ()) {
						return true;
					}
				} else if (circle.center.chunkToRegion().getZ() == region.getZ()) {
					if (circle.center.getX() < bottomLeft.getX() && rightMost.getX() >= bottomLeft.getX()) {
						return true;
					}
					if (circle.center.getX() > topRight.getX() && leftMost.getX() <= topRight.getX()) {
						return true;
					}
				}
			}
			return false;
		case CONTAINS_NOT:
			// if any chunk in the region is not part of the circles, we return true
			// if every chunk of the region is part of the circles, we return false

			// check if any corner overlaps with the circles
			for (CircleFilterDefinition circle : value) {
				// if this circle overlaps with all 4 corners, this region is fully covered
				if (circle.matches(topLeft) && circle.matches(bottomLeft) && circle.matches(bottomRight) && circle.matches(topRight)) {
					return false;
				}
			}
		}
		return true;
	}

	public static class CircleFilterDefinition implements Serializable {

		Point2i center;
		int radius;

		public CircleFilterDefinition(Point2i center, int radius) {
			this.center = center;
			this.radius = radius;
		}

		@Override
		public String toString() {
			return center.getX() + ";" + center.getZ() + ";" + radius;
		}

		@Override
		public CircleFilterDefinition clone() {
			return new CircleFilterDefinition(center.clone(), radius);
		}

		public boolean matches(Point2i p) {
			if (p.equals(center)) {
				return true;
			}
			double distSquared = Math.pow(p.getX() - center.getX(), 2) + Math.pow(p.getZ() - center.getZ(), 2);
			return distSquared <= ((double) radius + 0.3) * ((double) radius + 0.3);
		}
	}

	@Override
	public String toString() {
		return "Circle " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}
}
