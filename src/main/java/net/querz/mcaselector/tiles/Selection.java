package net.querz.mcaselector.tiles;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.point.Point2i;
import java.io.Serializable;

public class Selection implements Serializable {

	private final Long2ObjectOpenHashMap<LongOpenHashSet> selection;
	private final boolean inverted;
	private Point2i min, max;
	private final WorldDirectories world;

	public Selection(Long2ObjectOpenHashMap<LongOpenHashSet> selection, boolean inverted, WorldDirectories world) {
		this.selection = selection;
		this.inverted = inverted;
		this.world = world;
		calculateMinMax();
	}

	public Long2ObjectOpenHashMap<LongOpenHashSet> getSelectionData() {
		return selection;
	}

	public boolean isInverted() {
		return inverted;
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

	public WorldDirectories getWorld() {
		return world;
	}

	private void calculateMinMax() {
		min = new Point2i(Integer.MAX_VALUE, Integer.MAX_VALUE);
		max = new Point2i(Integer.MIN_VALUE, Integer.MIN_VALUE);

		int minRegionX = Integer.MAX_VALUE;
		int minRegionZ = Integer.MAX_VALUE;
		int maxRegionX = Integer.MIN_VALUE;
		int maxRegionZ = Integer.MIN_VALUE;

		for (Long2ObjectMap.Entry<LongOpenHashSet> entry : selection.long2ObjectEntrySet()) {
			Point2i region = new Point2i(entry.getLongKey());
			if (region.getX() <= minRegionX) {
				if (entry.getValue() == null) {
					min.setX(region.regionToChunk().getX());
				} else {
					for (long chunk : entry.getValue()) {
						Point2i c = new Point2i(chunk);
						if (c.getX() < min.getX()) {
							min.setX(c.getX());
						}
					}
				}
				minRegionX = region.getX();
			}
			if (region.getZ() <= minRegionZ) {
				if (entry.getValue() == null) {
					min.setZ(region.regionToChunk().getZ());
				} else {
					for (long chunk : entry.getValue()) {
						Point2i c = new Point2i(chunk);
						if (c.getZ() < min.getZ()) {
							min.setZ(c.getZ());
						}
					}
				}
				minRegionZ = region.getZ();
			}
			if (region.getX() >= maxRegionX) {
				if (entry.getValue() == null) {
					max.setX(region.regionToChunk().getX() + 31);
				} else {
					for (long chunk : entry.getValue()) {
						Point2i c = new Point2i(chunk);
						if (c.getX() > max.getX()) {
							max.setX(c.getX());
						}
					}
				}
				maxRegionX = region.getX();
			}
			if (region.getZ() >= maxRegionZ) {
				if (entry.getValue() == null) {
					max.setZ(region.regionToChunk().getZ() + 31);
				} else {
					for (long chunk : entry.getValue()) {
						Point2i c = new Point2i(chunk);
						if (c.getZ() > max.getZ()) {
							max.setZ(c.getZ());
						}
					}
				}
				maxRegionZ = region.getZ();
			}
		}
	}
}
