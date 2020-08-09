package net.querz.mcaselector.tiles;

import net.querz.mcaselector.point.Point2i;
import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class Selection implements Serializable {

	private final Map<Point2i, Set<Point2i>> selection;
	private Point2i min, max;
	private final File world;

	public Selection(Map<Point2i, Set<Point2i>> selection, File world) {
		this.selection = selection;
		this.world = world;
		calculateMinMax();
	}

	public Map<Point2i, Set<Point2i>> getSelectionData() {
		return selection;
	}

	public Point2i getMin() {
		if (min == null) {
			calculateMinMax();
		}
		return min;
	}

	public Point2i getMax() {
		if (max == null) {
			calculateMinMax();
		}
		return max;
	}

	public File getWorld() {
		return world;
	}

	private void calculateMinMax() {
		min = new Point2i(Integer.MAX_VALUE, Integer.MAX_VALUE);
		max = new Point2i(Integer.MIN_VALUE, Integer.MIN_VALUE);

		int minRegionX = Integer.MAX_VALUE;
		int minRegionZ = Integer.MAX_VALUE;
		int maxRegionX = Integer.MIN_VALUE;
		int maxRegionZ = Integer.MIN_VALUE;

		for (Map.Entry<Point2i, Set<Point2i>> entry : selection.entrySet()) {
			if (entry.getKey().getX() <= minRegionX) {
				if (entry.getValue() == null) {
					min.setX(entry.getKey().regionToChunk().getX());
				} else {
					for (Point2i chunk : entry.getValue()) {
						if (chunk.getX() < min.getX()) {
							min.setX(chunk.getX());
						}
					}
				}
				minRegionX = entry.getKey().getX();
			}
			if (entry.getKey().getZ() <= minRegionZ) {
				if (entry.getValue() == null) {
					min.setZ(entry.getKey().regionToChunk().getZ());
				} else {
					for (Point2i chunk : entry.getValue()) {
						if (chunk.getZ() < min.getZ()) {
							min.setZ(chunk.getZ());
						}
					}
				}
				minRegionZ = entry.getKey().getZ();
			}
			if (entry.getKey().getX() >= maxRegionX) {
				if (entry.getValue() == null) {
					max.setX(entry.getKey().regionToChunk().getX() + 31);
				} else {
					for (Point2i chunk : entry.getValue()) {
						if (chunk.getX() > max.getX()) {
							max.setX(chunk.getX());
						}
					}
				}
				maxRegionX = entry.getKey().getX();
			}
			if (entry.getKey().getZ() >= maxRegionZ) {
				if (entry.getValue() == null) {
					max.setZ(entry.getKey().regionToChunk().getZ() + 31);
				} else {
					for (Point2i chunk : entry.getValue()) {
						if (chunk.getZ() > max.getZ()) {
							max.setZ(chunk.getZ());
						}
					}
				}
				maxRegionZ = entry.getKey().getZ();
			}
		}
	}
}
