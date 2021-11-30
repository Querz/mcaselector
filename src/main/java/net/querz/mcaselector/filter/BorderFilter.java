package net.querz.mcaselector.filter;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMaps;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.tag.StringTag;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class BorderFilter extends IntFilter {

	protected final Object regionBufferLock;
	protected Long2ObjectSortedMap<RegionMCAFile> regionBuffer;

	public BorderFilter() {
		this(Operator.AND, Comparator.LARGER, 0, new Object());
	}

	private BorderFilter(Operator operator, Comparator comparator, int value, Object lock) {
		super(FilterType.BORDER, operator, comparator, value);
		regionBufferLock = lock;
		regionBuffer = Long2ObjectSortedMaps.synchronize(new Long2ObjectLinkedOpenHashMap<>(32), regionBufferLock);
	}

	@Override
	protected Integer getNumber(ChunkData data) {
		// if there is no data, we don't have to do anything
		if (data.getRegion() == null || data.getRegion().getData() == null) {
			return 5;
		}

		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		StringTag tag = chunkFilter.getStatus(data.getRegion().getData());
		if (tag == null || !tag.getValue().equals("full")) {
			return 5;
		}

		int count = 0;

		Point2i location = data.getRegion().getAbsoluteLocation();
		Point2i relative = getRelativeChunk(location);
		RegionMCAFile self = getRegionHeader(location.chunkToRegion());
		if (self == null) {
			// shouldn't happen
			System.out.println("shouldn't happen");
			return 5;
		}

		boolean print = location.equals(new Point2i(-15, 28));
		if (print) {
			System.out.println("absolute: " + location);
			System.out.println("relative: " + relative);
		}

		// check if this chunk is at the left-most border of this region
		if (relative.getX() % 32 == 0) {
			Point2i left = location.add(-1, 0);
			RegionMCAFile regionMCAFile = getRegionHeader(left.chunkToRegion());
			if (regionMCAFile == null || !regionMCAFile.hasChunkIndex(left)) {
				if (print) {
					System.out.println("empty chunk left");
				}
				count++;
			}
		} else if (!self.hasChunkIndex(location.add(-1, 0))) {
			if (print) {
				System.out.println("empty chunk left (self)");
			}
			count++;
		}

		// check if this chunk is at the right-most border of this region
		if (relative.getX() % 32 == 31) {
			Point2i right = location.add(1, 0);
			RegionMCAFile regionMCAFile = getRegionHeader(right.chunkToRegion());
			if (regionMCAFile == null || !regionMCAFile.hasChunkIndex(right)) {
				if (print) {
					System.out.println("empty chunk right");
				}
				count++;
			}
		} else if (!self.hasChunkIndex(location.add(1, 0))) {
			if (print) {
				System.out.println("empty chunk right (self)");
			}
			count++;
		}

		// check if this chunk is at the top-most border of this region
		if (relative.getZ() % 32 == 0) {
			Point2i top = location.add(0, -1);
			RegionMCAFile regionMCAFile = getRegionHeader(top.chunkToRegion());
			if (regionMCAFile == null || !regionMCAFile.hasChunkIndex(top)) {
				if (print) {
					System.out.println("empty chunk top");
				}
				count++;
			}
		} else if (!self.hasChunkIndex(location.add(0, -1))) {
			if (print) {
				System.out.println("empty chunk top (self)");
			}
			count++;
		}

		// check if this chunk is at the bottom-most border of this region
		if (relative.getZ() % 32 == 31) {
			Point2i bottom = location.add(0, 1);
			RegionMCAFile regionMCAFile = getRegionHeader(bottom.chunkToRegion());
			if (regionMCAFile == null || !regionMCAFile.hasChunkIndex(bottom)) {
				if (print) {
					System.out.println("empty chunk bottom");
				}
				count++;
			}
		} else if (!self.hasChunkIndex(location.add(0, 1))) {
			if (print) {
				System.out.println("empty chunk bottom (self)");
			}
			count++;
		}

		return count;
	}

	private Point2i getRelativeChunk(Point2i chunkCoordinate) {
		return new Point2i((chunkCoordinate.getX() & 0x1F), (chunkCoordinate.getZ() & 0x1F));
	}

	private RegionMCAFile getRegionHeader(Point2i region) {
		long key = region.asLong();

		synchronized (regionBufferLock) {
			if (regionBuffer.containsKey(key)) {
				return regionBuffer.get(key);
			}

			// load region header
			RegionMCAFile regionMCAFile = new RegionMCAFile(FileHelper.createMCAFilePath(region));
			if (!regionMCAFile.getFile().exists() || regionMCAFile.getFile().length() <= 8192) {
				push(key, null);
				return null;
			}
			byte[] data = new byte[(int) regionMCAFile.getFile().length()];
			try (InputStream is = Files.newInputStream(regionMCAFile.getFile().toPath(), StandardOpenOption.READ)) {
				is.read(data);
				regionMCAFile.loadBorderChunks(new ByteArrayPointer(data));
			} catch (IOException ex) {
				Debug.dumpException("failed to read data from " + regionMCAFile.getFile(), ex);
				push(key, null);
				return null;
			}

			push(key, regionMCAFile);
			return regionMCAFile;
		}
	}

	private void push(long key, RegionMCAFile regionMCAFile) {
		if (regionBuffer.size() == 32) {
			regionBuffer.remove(regionBuffer.firstLongKey());
		}
		regionBuffer.put(key, regionMCAFile);
	}

	@Override
	public void setFilterValue(String raw) {
		super.setFilterValue(raw);
		if (isValid() && (getFilterValue() < 0 || getFilterValue() > 4)) {
			setFilterNumber(0);
			setValid(false);
		}
	}

	@Override
	public BorderFilter clone() {
		BorderFilter bf = new BorderFilter(getOperator(), getComparator(), value, regionBufferLock);
		bf.regionBuffer = regionBuffer;
		return bf;
	}

	@Override
	public void resetTempData() {
		regionBuffer.clear();
	}

	@Override
	public boolean selectionOnly() {
		return true;
	}
}
