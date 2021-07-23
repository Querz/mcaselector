package net.querz.mcaselector.io;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.Tile;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectionData {

	private final Map<Point2i, Set<Point2i>> selection;
	private final boolean inverted;

	public SelectionData(Map<Point2i, Set<Point2i>> selection, boolean inverted) {
		this.selection = selection;
		this.inverted = inverted;
	}

	public Map<Point2i, Set<Point2i>> selection() {
		return selection;
	}

	public boolean inverted() {
		return inverted;
	}

	public boolean isEmpty() {
		return !inverted && selection.isEmpty();
	}

	public boolean isRegionSelected(Point2i region) {
		// a region is selected if:
		// - the selection contains the region and it's not inverted
		// - the selection contains the region and the chunks are not null and it's inverted
		// - the selection does not contain the region and it's inverted
		return selection != null && (selection.containsKey(region) && !inverted || selection.containsKey(region) && selection.get(region) != null && inverted || !selection.containsKey(region) && inverted);
	}

	public static void main(String[] args) {
		Map<Point2i, Set<Point2i>> sel = new HashMap<>();
		sel.put(new Point2i(0, 0), Collections.singleton(new Point2i(0, 0)));

		SelectionData s = new SelectionData(sel, false);

		System.out.println(s.isChunkSelected(new Point2i(0, 0)));
	}


	public boolean isChunkSelected(Point2i chunk) {

		if (selection == null) {
			return false;
		}

		Point2i region = chunk.chunkToRegion();
		boolean containsRegion = selection.containsKey(region);
		Set<Point2i> chunks = selection.get(region);

		// a chunk is selected if:
		// - the selection does not contain the region and it's inverted
		// - the selection contains the region and the chunks are null and it's inverted
		// - the selection contains the region and the chunk and it's not inverted
		// - the selection contains the region and not the chunk and it's inverted

		return !containsRegion && inverted
				|| containsRegion && chunks == null && inverted
				|| containsRegion && chunks != null && chunks.contains(chunk) && !inverted
				|| containsRegion && chunks != null && !chunks.contains(chunk) && inverted;
	}

	public static Set<Point2i> createInvertedRegionSet(Point2i region, Set<Point2i> selectedChunks) {
		// null (all chunks selected) --> empty set (no chunks selected)
		if (selectedChunks == null) {
			return Collections.emptySet();
		}
		// empty set (no chunks selected) --> null (all chunks selected)
		if (selectedChunks.isEmpty()) {
			return null;
		}
		// set the initial capacity precisely because we can
		Set<Point2i> invertedSelectedChunks = new HashSet<>(Tile.CHUNKS - selectedChunks.size());
		Point2i regionChunk = region.regionToChunk();
		for (int x = regionChunk.getX(); x < regionChunk.getX() + 32; x++) {
			for (int z = regionChunk.getZ(); z < regionChunk.getZ() + 32; z++) {
				Point2i chunk = new Point2i(x, z);
				if (!selectedChunks.contains(chunk)) {
					invertedSelectedChunks.add(chunk);
				}
			}
		}
		return invertedSelectedChunks;
	}

	@Override
	public String toString() {
		return "i=" + inverted + ":" + selection;
	}
}
