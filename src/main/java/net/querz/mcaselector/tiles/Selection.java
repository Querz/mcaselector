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

		Point2i minRegionX = new Point2i(Integer.MAX_VALUE, 0);
		Point2i minRegionZ = new Point2i(0, Integer.MAX_VALUE);
		Point2i maxRegionX = new Point2i(Integer.MIN_VALUE, 0);
		Point2i maxRegionZ = new Point2i(0, Integer.MIN_VALUE);

		for (Map.Entry<Point2i, Set<Point2i>> entry : selection.entrySet()) {
			if (entry.getKey().getX() < minRegionX.getX()) {
				minRegionX = entry.getKey();
			}
			if (entry.getKey().getY() < minRegionZ.getY()) {
				minRegionZ = entry.getKey();
			}
			if (entry.getKey().getX() > maxRegionX.getX()) {
				maxRegionX = entry.getKey();
			}
			if (entry.getKey().getY() > maxRegionZ.getY()) {
				maxRegionZ = entry.getKey();
			}
		}

		Set<Point2i> minRegionXChunks = selection.get(minRegionX);
		if (minRegionXChunks == null) {
			min.setX(minRegionX.regionToChunk().getX());
		} else {
			for (Point2i chunk : minRegionXChunks) {
				if (chunk.getX() < min.getX()) {
					min.setX(chunk.getX());
				}
			}
		}

		Set<Point2i> minRegionZChunks = selection.get(minRegionZ);
		if (minRegionZChunks == null) {
			min.setY(minRegionZ.regionToChunk().getY());
		} else {
			for (Point2i chunk : minRegionZChunks) {
				if (chunk.getY() < min.getY()) {
					min.setX(chunk.getY());
				}
			}
		}

		Set<Point2i> maxRegionXChunks = selection.get(maxRegionX);
		if (maxRegionXChunks == null) {
			max.setX(maxRegionX.regionToChunk().getX() + 31);
		} else {
			for (Point2i chunk : maxRegionXChunks) {
				if (chunk.getX() > max.getX()) {
					max.setX(chunk.getX());
				}
			}
		}

		Set<Point2i> maxRegionZChunks = selection.get(maxRegionZ);
		if (maxRegionZChunks == null) {
			max.setY(maxRegionZ.regionToChunk().getY() + 31);
		} else {
			for (Point2i chunk : maxRegionZChunks) {
				if (chunk.getY() > max.getY()) {
					max.setY(chunk.getY());
				}
			}
		}
	}


}
