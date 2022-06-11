package net.querz.mcaselector.selection;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.point.Point2i;
import java.io.Serializable;

// SelectionData is used to transfer selection and world data for the copy-/paste function
public class SelectionData implements Serializable {

	private final Selection selection;
	private Point2i min, max;
	private final WorldDirectories world;

	public SelectionData(Selection selection, WorldDirectories world) {
		this.selection = selection;
		this.world = world;
	}

	public Selection getSelection() {
		return selection;
	}

	public boolean isInverted() {
		return selection.inverted;
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

	public long getWidth() {
		if (min == null || max == null) {
			calculateMinMax();
		}
		return Math.abs((long) max.getX() - (long) min.getX()) + 1L;
	}

	public long getHeight() {
		if (min == null || max == null) {
			calculateMinMax();
		}
		return Math.abs((long) max.getZ() - (long) min.getZ()) + 1L;
	}

	public int size() {
		return selection.size();
	}

	public WorldDirectories getWorld() {
		return world;
	}

	protected void calculateMinMax() {
		int minChunkX = 31;
		int minChunkZ = 31;
		int maxChunkX = 0;
		int maxChunkZ = 0;

		int minRegionX = Integer.MAX_VALUE;
		int minRegionZ = Integer.MAX_VALUE;
		int maxRegionX = Integer.MIN_VALUE;
		int maxRegionZ = Integer.MIN_VALUE;

		for (Long2ObjectMap.Entry<ChunkSet> entry : selection.selection.long2ObjectEntrySet()) {
			Point2i region = new Point2i(entry.getLongKey());
			if (region.getX() <= minRegionX) {
				if (region.getX() < minRegionX) {
					minRegionX = region.getX();
					minChunkX = 31;
				}
				minChunkX = entry.getValue() == null ? 0 : entry.getValue().getMinX(minChunkX);
			}
			if (region.getX() >= maxRegionX) {
				if (region.getX() > maxRegionX) {
					maxRegionX = region.getX();
					maxChunkX = 0;
				}
				maxChunkX = entry.getValue() == null ? 31 : entry.getValue().getMaxX(maxChunkX);
			}
			if (region.getZ() <= minRegionZ) {
				if (region.getZ() < minRegionZ) {
					minRegionZ = region.getZ();
					minChunkZ = 31;
				}
				minChunkZ = entry.getValue() == null ? 0 : entry.getValue().getMinZ(minChunkZ);
			}
			if (region.getZ() >= maxRegionZ) {
				if (region.getZ() > maxRegionZ) {
					maxRegionZ = region.getZ();
					maxChunkZ = 0;
				}
				maxChunkZ = entry.getValue() == null ? 31 : entry.getValue().getMaxZ(maxChunkZ);
			}
		}
		min = new Point2i(minRegionX, minRegionZ).regionToChunk().add(minChunkX, minChunkZ);
		max = new Point2i(maxRegionX, maxRegionZ).regionToChunk().add(maxChunkX, maxChunkZ);
	}

	@Override
	public String toString() {
		return String.format("SelectionData{min: %s, max: %s, width: %d, height: %d}", min, max, getWidth(), getHeight());
	}
}
