package net.querz.mcaselector.io;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.Tile;

public record SelectionData(Long2ObjectOpenHashMap<LongOpenHashSet> selection, boolean inverted) {

	public boolean isEmpty() {
		return !inverted && selection.isEmpty();
	}

	public boolean isRegionSelected(Point2i region) {
		// a region is selected if:
		// - the selection contains the region and it's not inverted
		// - the selection contains the region and the chunks are not null and it's inverted
		// - the selection does not contain the region and it's inverted
		return isRegionSelected(region.asLong());
	}

	public boolean isRegionSelected(long region) {
		return selection != null && (selection.containsKey(region) && !inverted || selection.containsKey(region) && selection.get(region) != null && inverted || !selection.containsKey(region) && inverted);
	}

	public boolean isChunkSelected(Point2i chunk) {

		if (selection == null) {
			return false;
		}

		Point2i region = chunk.chunkToRegion();
		long l = region.asLong();
		long c = chunk.asLong();
		boolean containsRegion = selection.containsKey(l);
		LongOpenHashSet chunks = selection.get(l);

		// a chunk is selected if:
		// - the selection does not contain the region and it's not inverted
		// - the selection contains the region and the chunks are null and it's inverted
		// - the selection contains the region and the chunk and it's not inverted
		// - the selection contains the region and not the chunk and it's inverted

		return !containsRegion && inverted
				|| containsRegion && chunks == null && !inverted
				|| containsRegion && chunks != null && chunks.contains(c) && !inverted
				|| containsRegion && chunks != null && !chunks.contains(c) && inverted;
	}

	public static LongOpenHashSet createInvertedRegionSet(long region, LongOpenHashSet selectedChunks) {
		return createInvertedRegionSet(new Point2i(region), selectedChunks);
	}

	public static LongOpenHashSet createInvertedRegionSet(Point2i region, LongOpenHashSet selectedChunks) {
		// null (all chunks selected) --> empty set (no chunks selected)
		if (selectedChunks == null) {
			return LongOpenHashSet.of();
		}
		// empty set (no chunks selected) --> null (all chunks selected)
		if (selectedChunks.isEmpty()) {
			return null;
		}
		// set the initial capacity precisely because we can
		LongOpenHashSet invertedSelectedChunks = new LongOpenHashSet(Tile.CHUNKS - selectedChunks.size());
		Point2i regionChunk = region.regionToChunk();
		for (int x = regionChunk.getX(); x < regionChunk.getX() + 32; x++) {
			for (int z = regionChunk.getZ(); z < regionChunk.getZ() + 32; z++) {
				long chunk = new Point2i(x, z).asLong();
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
