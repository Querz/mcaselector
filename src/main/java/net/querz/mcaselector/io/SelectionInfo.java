package net.querz.mcaselector.io;

import net.querz.mcaselector.point.Point2i;

import java.util.function.Consumer;

public class SelectionInfo {

	private long width;
	private long height;
	private Point2i min;
	private Point2i max;

	public SelectionInfo(Point2i min, Point2i max) {
		this.min = min;
		setMax(max);
	}

	public long getWidth() {
		return width;
	}

	public long getHeight() {
		return height;
	}

	public Point2i getMin() {
		return min;
	}

	public void setMin(Point2i min) {
		this.min = min;
		width = Math.abs((long) max.getX() - (long) min.getX()) + 1L;
		height = Math.abs((long) max.getZ() - (long) min.getZ()) + 1L;
	}

	public Point2i getMax() {
		return max;
	}

	public void setMax(Point2i max) {
		this.max = max;
		width = Math.abs((long) max.getX() - (long) min.getX()) + 1L;
		height = Math.abs((long) max.getZ() - (long) min.getZ()) + 1L;
	}

	public void iterateRegionChunks(Point2i region, Consumer<Point2i> chunkConsumer) {
		Point2i regionChunk = region.regionToChunk();
		for (int x = Math.max(regionChunk.getX(), min.getX()); x <= Math.min(regionChunk.getX() + 31, max.getX()); x++) {
			for (int z = Math.max(regionChunk.getZ(), min.getZ()); z <= Math.min(regionChunk.getZ() + 31, max.getZ()); z++) {
				chunkConsumer.accept(new Point2i(x, z));
			}
		}
	}

	public Point2i getPointInSelection(Point2i chunk) {
		return chunk.sub(min);
	}

	public boolean isChunkInSelection(Point2i chunk) {
		return chunk.getX() <= max.getX() && chunk.getX() >= min.getX() && chunk.getZ() <= max.getZ() && chunk.getZ() >= min.getZ();
	}

	@Override
	public String toString() {
		return String.format("SelectionInfo{min: %s, max: %s, width: %d, height: %d}", min, max, width, height);
	}

}
