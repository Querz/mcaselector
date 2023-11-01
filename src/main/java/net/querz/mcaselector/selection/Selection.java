package net.querz.mcaselector.selection;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.point.Point2i;
import java.io.*;
import java.util.Iterator;

public class Selection implements Serializable, Iterable<Long2ObjectMap.Entry<ChunkSet>> {

	protected Long2ObjectOpenHashMap<ChunkSet> selection;
	protected boolean inverted;

	public Selection() {
		this.selection = new Long2ObjectOpenHashMap<>();
		this.inverted = false;
	}

	protected Selection(Long2ObjectOpenHashMap<ChunkSet> selection, boolean inverted) {
		this.selection = selection;
		this.inverted = inverted;
	}

	public static Selection readFromFile(File csvFile) throws IOException {
		Long2ObjectOpenHashMap<ChunkSet> sel = new Long2ObjectOpenHashMap<>();
		Selection selection = new Selection(sel, false);
		boolean inverted = false;
		try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
			String line;
			int num = 0;
			while ((line = br.readLine()) != null) {
				num++;
				if (num == 1 && "inverted".equals(line)) {
					inverted = true;
					continue;
				}

				String[] elements = line.split(";");
				if (elements.length != 2 && elements.length != 4) {
					throw ioException("invalid region or chunk coordinate format in line %d", num);
				}

				Integer x = parseInt(elements[0]);
				Integer z = parseInt(elements[1]);
				if (x == null || z == null) {
					throw ioException("failed to read region coordinates in line %d", num);
				}

				Point2i region = new Point2i(x, z);
				if (elements.length == 4) {
					Integer cx = parseInt(elements[2]);
					Integer cz = parseInt(elements[3]);
					if (cx == null || cz == null) {
						throw ioException("failed to read chunk coordinates in line %d", num);
					}

					// check if this chunk is actually in this region
					Point2i chunk = new Point2i(cx, cz);
					if (!chunk.chunkToRegion().equals(region)) {
						throw ioException("chunk %s is not in region %s in line %d", chunk, region, num);
					}
					selection.addChunk(chunk);
				} else {
					selection.addRegion(region.asLong());
				}
			}
		}
		selection.setInverted(inverted);
		return selection;
	}

	public void saveToFile(File csvFile) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
			if (inverted) {
				bw.write("inverted\n");
			}
			for (Long2ObjectMap.Entry<ChunkSet> entry : selection.long2ObjectEntrySet()) {
				Point2i region = new Point2i(entry.getLongKey());
				if (entry.getValue() == null) {
					writePoint(bw, region);
					bw.write('\n');
					continue;
				}
				for (int i : entry.getValue()) {
					writePoint(bw, region);
					bw.write(';');
					Point2i c = new Point2i(i).add(region.regionToChunk());
					writePoint(bw, c);
					bw.write('\n');
				}
			}
		}
	}

	public String saveToString() {
		StringBuilder sb = new StringBuilder();
		if (inverted) {
			sb.append("inverted\n");
		}
		for (Long2ObjectMap.Entry<ChunkSet> entry : selection.long2ObjectEntrySet()) {
			Point2i region = new Point2i(entry.getLongKey());
			if (entry.getValue() == null) {
				sb.append(writePoint(region));
				sb.append('\n');
				continue;
			}
			for (int i : entry.getValue()) {
				sb.append(writePoint(region));
				sb.append(';');
				Point2i c = new Point2i(i).add(region.regionToChunk());
				sb.append(writePoint(c));
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	private static void writePoint(BufferedWriter bw, Point2i p) throws IOException {
		bw.write(Integer.toString(p.getX()));
		bw.write(';');
		bw.write(Integer.toString(p.getZ()));
	}

	private static String writePoint(Point2i p) {
		return p.getX() + ";" + p.getZ();
	}

	private static IOException ioException(String msg, Object... format) {
		return new IOException(String.format(msg, format));
	}

	private static Integer parseInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public boolean isChunkSelected(int x, int z) {
		Point2i pChunk = new Point2i(x, z);
		Point2i pRegion = pChunk.chunkToRegion();
		return isChunkSelected(pRegion.asLong(), pChunk.asChunkIndex());
	}

	public boolean isChunkSelected(Point2i chunk) {
		return isChunkSelected(chunk.chunkToRegion().asLong(), chunk.asChunkIndex());
	}

	protected boolean isChunkSelected(long region, short chunk) {
		if (selection.containsKey(region)) {
			ChunkSet chunks = selection.get(region);
			return (chunks == null || chunks.get(chunk)) != inverted;
		} else {
			return inverted;
		}
	}

	public boolean isInverted() {
		return inverted;
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}

	public boolean isEmpty() {
		return !inverted && selection.isEmpty();
	}

	public int size() {
		return inverted ? Integer.MAX_VALUE : selection.size();
	}

	public int count() {
		int result = 0;
		for (Long2ObjectMap.Entry<ChunkSet> entry : selection.long2ObjectEntrySet()) {
			result += entry.getValue() == null ? 1024 : entry.getValue().size();
		}
		return result;
	}

	public Point2i one() {
		if (inverted || selection.isEmpty()) {
			return new Point2i();
		}
		return new Point2i(selection.long2ObjectEntrySet().iterator().next().getLongKey());
	}

	public void addChunk(Point2i chunk) {
		addChunk(chunk.chunkToRegion().asLong(), chunk.asChunkIndex());
	}

	public Selection getTrueSelection(WorldDirectories world) {
		if (!inverted) {
			return this;
		}

		Long2ObjectOpenHashMap<ChunkSet> selection = new Long2ObjectOpenHashMap<>();
		LongOpenHashSet allRegions = FileHelper.parseAllMCAFileNames(world.getRegion());
		LongOpenHashSet allPoi = FileHelper.parseAllMCAFileNames(world.getPoi());
		LongOpenHashSet allEntities = FileHelper.parseAllMCAFileNames(world.getEntities());
		allRegions.addAll(allPoi);
		allRegions.addAll(allEntities);
		for (long region : allRegions) {
			// if a region is fully selected, we add null to the result
			if (!this.selection.containsKey(region)) {
				selection.put(region, null);
				continue;
			}

			// if a region is partially selected, we use the invert function
			ChunkSet chunks;
			if ((chunks = this.selection.get(region)) != null) {
				selection.put(region, invertChunks(chunks));
			}
		}
		return new Selection(selection, false);
	}

	public void addChunk(long chunkCoords) {
		Point2i chunk = new Point2i(chunkCoords);
		addChunk(chunk.chunkToRegion().asLong(), chunk.asChunkIndex());
	}

	public void setSelection(Selection other) {
		this.selection = other.selection;
		this.inverted = other.inverted;
	}

	protected void addChunk(long region, short chunk) {
		if (inverted) {
			if (selection.containsKey(region)) {
				ChunkSet chunks = selection.get(region);
				if (chunks == null) {
					chunks = new ChunkSet();
					chunks.fill();
					chunks.clear(chunk);
					selection.put(region, chunks);
				} else {
					chunks.clear(chunk);
					if (chunks.size() == 0) {
						selection.remove(region);
					}
				}
			}
		} else {
			if (selection.containsKey(region)) {
				ChunkSet chunks = selection.get(region);
				if (chunks != null) {
					chunks.set(chunk);
					if (chunks.size() == 1024) {
						selection.put(region, null);
					}
				}
			} else {
				ChunkSet chunks = new ChunkSet();
				chunks.set(chunk);
				selection.put(region, chunks);
			}
		}
	}

	// returns the number of added chunks
	public int addRegion(long region) {
		if (inverted) {
			if (selection.containsKey(region)) {
				ChunkSet chunks = selection.get(region);
				selection.remove(region);
				if (chunks == null) {
					return 1024;
				}
				return chunks.size();
			} else {
				return 0;
			}
		} else {
			if (selection.containsKey(region)) {
				ChunkSet chunks = selection.get(region);
				if (chunks == null) {
					return 0;
				}
				selection.put(region, null);
				return 1024 - chunks.size();
			} else {
				selection.put(region, null);
				return 1024;
			}
		}
	}

	public boolean isRegionSelected(long region) {
		if (inverted) {
			return !selection.containsKey(region);
		} else {
			return selection.containsKey(region) && selection.get(region) == null;
		}
	}

	public boolean isAnyChunkInRegionSelected(long region) {
		if (inverted) {
			return !selection.containsKey(region) || selection.get(region) != null;
		} else {
			return selection.containsKey(region);
		}
	}

	public boolean isAnyChunkInRegionSelected(Point2i region) {
		return isAnyChunkInRegionSelected(region.asLong());
	}

	public ChunkSet getSelectedChunks(Point2i region) {
		if (inverted) {
			if (selection.containsKey(region.asLong())) {
				return invertChunks(selection.get(region.asLong())).immutable();
			} else {
				return null;
			}
		}
		if (selection.containsKey(region.asLong())) {
			ChunkSet result = selection.get(region.asLong());
			if (result == null) {
				return null;
			}
			return result.immutable();
		} else {
			return ChunkSet.EMPTY_SET;
		}
	}

	public void clear() {
		selection = new Long2ObjectOpenHashMap<>();
		inverted = false;
	}

	// returns how many chunks have been removed
	public int removeRegion(long region) {
		if (inverted) {
			if (selection.containsKey(region)) {
				ChunkSet chunks = selection.get(region);
				if (chunks == null) {
					return 0;
				}
				selection.put(region, null);
				return 1024 - chunks.size();
			} else {
				selection.put(region, null);
				return 1024;
			}
		} else {
			if (selection.containsKey(region)) {
				ChunkSet chunks = selection.get(region);
				selection.remove(region);
				if (chunks == null) {
					return 1024;
				}
				return chunks.size();
			} else {
				return 0;
			}
		}
	}

	public void removeChunk(Point2i chunk) {
		long region = chunk.chunkToRegion().asLong();
		if (inverted) {
			if (selection.containsKey(region)) {
				ChunkSet chunks = selection.get(region);
				if (chunks != null) {
					chunks.set(chunk.asChunkIndex());
					if (chunks.size() == 1024) {
						selection.put(region, null);
					}
				}
			} else {
				ChunkSet chunks = new ChunkSet();
				chunks.set(chunk.asChunkIndex());
				selection.put(region, chunks);
			}
		} else {
			if (selection.containsKey(region)) {
				ChunkSet chunks = selection.get(region);
				if (chunks == null) {
					chunks = new ChunkSet();
					chunks.fill();
					chunks.clear(chunk.asChunkIndex());
					selection.put(region, chunks);
				} else {
					chunks.clear(chunk.asChunkIndex());
					if (chunks.size() == 0) {
						selection.remove(region);
					}
				}
			}
		}
	}

	public ChunkSet getSelectedChunksIgnoreInverted(Point2i region) {
		if (selection.containsKey(region.asLong())) {
			return selection.get(region.asLong());
		} else {
			return ChunkSet.EMPTY_SET;
		}
	}

	public void invertAll() {
		for (Long2ObjectMap.Entry<ChunkSet> entry : selection.long2ObjectEntrySet()) {
			if (entry.getValue() == null) {
				selection.remove(entry.getLongKey());
			} else {
				selection.put(entry.getLongKey(), entry.getValue().flip());
			}
		}
	}

	private static ChunkSet invertChunks(ChunkSet chunks) {
		if (chunks == null) {
			return new ChunkSet();
		}
		ChunkSet result = new ChunkSet();
		for (short i = 0; i < 1024; i++) {
			if (!chunks.get(i)) {
				result.set(i);
			}
		}
		return result;
	}

	public void merge(Selection other) {
		if (!inverted && !other.inverted) {

			for (Long2ObjectMap.Entry<ChunkSet> entry : other.selection.long2ObjectEntrySet()) {
				long r = entry.getLongKey();
				if (selection.containsKey(r)) {
					selection.put(r, add(selection.get(r), entry.getValue()));
				} else {
					selection.put(r, cloneValue(entry.getValue()));
				}
			}
		} else if (inverted && !other.inverted) {
			// subtract all other chunks from this selection
			for (Long2ObjectMap.Entry<ChunkSet> entry : other.selection.long2ObjectEntrySet()) {
				long r = entry.getLongKey();
				if (selection.containsKey(r)) {
					ChunkSet result = subtract(selection.get(r), entry.getValue());
					if (result.size() == 0) {
						selection.remove(r);
					} else {
						selection.put(r, result);
					}
				}
			}
		} else if (!inverted) { // this selection is not inverted but the other is
			// if something is marked in the other selection, we add it to this selection
			// remember the regions we already touched and ignore them in the next loop
			for (Long2ObjectMap.Entry<ChunkSet> entry : other.selection.long2ObjectEntrySet()) {
				long r = entry.getLongKey();
				if (selection.containsKey(r)) {
					ChunkSet result;
					if ((result = subtract(cloneValue(entry.getValue()), selection.get(r))).size() == 0) {
						selection.remove(r);
					} else {
						selection.put(r, result);
					}
				} else {
					selection.put(r, cloneValue(entry.getValue()));
				}
			}

			// if something is marked in this selection, but not the other, we remove it so it's marked when inverted
			for (Long2ObjectMap.Entry<ChunkSet> entry : selection.long2ObjectEntrySet()) {
				long r = entry.getLongKey();
				if (!other.selection.containsKey(r)) {
					selection.remove(r);
				}
			}
			// invert this selection at the end
			inverted = true;
		} else { // both are inverted

			for (Long2ObjectMap.Entry<ChunkSet> entry : selection.long2ObjectEntrySet()) {
				long r = entry.getLongKey();
				if (!other.selection.containsKey(r)) {
					// region does not exist in other selection, so it is fully marked.
					// we have to delete it from this selection to mark it too.
					selection.remove(r);
				} else {
					// region exists in other selection so we need to union them
					ChunkSet union = union(entry.getValue(), other.selection.get(r));
					// and put it in this selection
					if (union != null && union.size() == 0) {
						// the union is completely selected, so we remove this region to fully mark it
						selection.remove(r);
					} else {
						selection.put(r, union);
					}
				}
			}
		}
	}

	private static ChunkSet cloneValue(ChunkSet v) {
		return v == null ? null : v.clone();
	}

	private static ChunkSet union(ChunkSet a, ChunkSet b) {
		if (a == null) {
			return cloneValue(b);
		}
		if (b == null) {
			return a;
		}
		a.forEach(l -> {
			if (!b.get(l)) {
				a.clear(l);
			}
		});
		b.forEach(l -> {
			if (!a.get(l)) {
				a.clear(l);
			}
		});
		return a;
	}

	private static ChunkSet subtract(ChunkSet source, ChunkSet target) {
		if (source == null) {
			return invertChunks(target);
		}
		if (target == null) {
			return new ChunkSet();
		}
		source.removeIf(target::get);
		return source;
	}

	private static ChunkSet add(ChunkSet source, ChunkSet target) {
		if (source == null || target == null) {
			return null;
		}
		source.or(target);
		return source.size() == 1024 ? null : source;
	}

	public void addAll(LongOpenHashSet entries) {
		for (long entry : entries) {
			addChunk(entry);
		}
	}

	public void addAll(Point2i region, ChunkSet chunks) {
		long r = region.asLong();
		if (inverted) {
			if (chunks == null || chunks.size() == 1024) {
				selection.remove(r);
			} else if (selection.containsKey(r)) {
				ChunkSet existing;
				if ((existing = selection.get(r)) != null) {
					existing.otherNotAnd(chunks);
					if (existing.isEmpty()) {
						selection.remove(r);
					}
				} else {
					ChunkSet inverted = invertChunks(chunks);
					if (!inverted.isEmpty()) {
						selection.put(r, inverted);
					}
				}
			}
		} else {
			if (chunks == null || chunks.size() == 1024) {
				selection.put(r, null);
			} else if (selection.containsKey(r)) {
				ChunkSet existing;
				if ((existing = selection.get(r)) != null) {
					existing.or(chunks);
					if (existing.size() == 1024) {
						selection.put(r, null);
					}
				}
			} else if (!chunks.isEmpty()) {
				selection.put(r, chunks);
			}
		}
	}

	public void addRadius(int radius, Selection bounds) {
		for (Long2ObjectMap.Entry<ChunkSet> entry : selection.long2ObjectEntrySet()) {
			// if the entry is already fully selected we only need to loop over the edges
			if (entry.getValue() == null) {
				for (int x = 0; x < 32; x++) {
					Point2i center = new Point2i(x, 0).add(new Point2i(entry.getLongKey()).regionToChunk());
					addRadius(center, radius, bounds);
					center = new Point2i(x, 31).add(new Point2i(entry.getLongKey()).regionToChunk());
					addRadius(center, radius, bounds);
				}
				for (int z = 1; z < 31; z++) {
					Point2i center = new Point2i(0, z).add(new Point2i(entry.getLongKey()).regionToChunk());
					addRadius(center, radius, bounds);
					center = new Point2i(31, z).add(new Point2i(entry.getLongKey()).regionToChunk());
					addRadius(center, radius, bounds);
				}
			} else {
				// make sure to loop over a clone of the ChunkSet, so we don't apply a radius on chunks that were not originally selected
				for (int i : entry.getValue().clone()) {
					// only apply radius if this chunk is adjacent to selected chunks

					// calculate the chunk location
					Point2i center = new Point2i(i).add(new Point2i(entry.getLongKey()).regionToChunk());
					addRadius(center, radius, bounds);
				}
			}
		}
	}

	private void addRadius(Point2i center, int radius, Selection bounds) {
		Point2i min = center.sub(radius);
		Point2i max = center.add(radius);
		double radiusSquared = ((double) radius + 0.3) * ((double) radius + 0.3);
		int distX, distZ;
		for (int x = min.getX(); x <= max.getX(); x++) {
			for (int z = min.getZ(); z <= max.getZ(); z++) {
				distX = x - center.getX();
				distZ = z - center.getZ();
				if ((bounds == null || bounds.isChunkSelected(x, z)) && distX * distX + distZ * distZ <= radiusSquared) {
					addChunk(new Point2i(x, z));
				}
			}
		}
	}

	@Override
	public Iterator<Long2ObjectMap.Entry<ChunkSet>> iterator() {
		return selection.long2ObjectEntrySet().iterator();
	}

	public Stats getStats() {
		int totalSelectedChunks = 0;
		int totalChunksOfPartiallySelectedRegions = 0;
		int partiallySelectedRegions = 0;
		int fullySelectedRegions = 0;
		int below64 = 0, below128 = 0, below256 = 0, below512 = 0;
		for (Long2ObjectMap.Entry<ChunkSet> entry : selection.long2ObjectEntrySet()) {
			if (entry.getValue() == null) {
				totalSelectedChunks += 1024;
				fullySelectedRegions++;
			} else {
				int size = entry.getValue().size();
				totalSelectedChunks += size;
				totalChunksOfPartiallySelectedRegions += size;
				partiallySelectedRegions++;
				if (size < 512) {
					below512++;
				}
				if (size < 256) {
					below256++;
				}
				if (size < 128) {
					below128++;
				}
				if (size < 64) {
					below64++;
				}
			}
		}
		return new Stats(
			totalSelectedChunks,
			totalChunksOfPartiallySelectedRegions,
			partiallySelectedRegions,
			fullySelectedRegions,
			below64, below128, below256, below512);
	}

	public record Stats(
		int totalSelectedChunks,
		int totalChunksOfPartiallySelectedRegions,
		int partiallySelectedRegions,
		int fullySelectedRegions,
		int below64, int below128, int below256, int below512) {}

	@Override
	public String toString() {
		return getStats().toString();
	}
}
