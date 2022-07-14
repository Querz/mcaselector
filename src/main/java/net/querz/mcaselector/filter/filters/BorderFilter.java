package net.querz.mcaselector.filter.filters;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMaps;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.IntFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.StringTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class BorderFilter extends IntFilter {

	private static final Logger LOGGER = LogManager.getLogger(BorderFilter.class);

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
		if (data.region() == null || data.region().getData() == null) {
			return 9;
		}

		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		StringTag tag = chunkFilter.getStatus(data.region().getData());
		if (tag == null || !tag.getValue().equals("full")) {
			return 9;
		}

		int count = 0;

		Point2i location = data.region().getAbsoluteLocation();
		Point2i relative = getRelativeChunk(location);
		RegionMCAFile self = getRegionHeader(location.chunkToRegion());
		if (self == null) {
			// shouldn't happen
			return 9;
		}

		RegionMCAFile mcaFileTop = null;
		RegionMCAFile mcaFileRight = null;
		RegionMCAFile mcaFileBottom = null;
		RegionMCAFile mcaFileLeft = null;
		RegionMCAFile mcaFileTopLeft = null;
		RegionMCAFile mcaFileTopRight = null;
		RegionMCAFile mcaFileBottomRight = null;
		RegionMCAFile mcaFileBottomLeft = null;

		// check if this chunk is at the left-most border of this region
		if (relative.getX() == 0) {
			Point2i left = location.add(-1, 0);
			mcaFileLeft = getRegionHeader(left.chunkToRegion());

			if (relative.getZ() == 0) {
				Point2i topLeft = location.add(-1, -1);
				mcaFileTopLeft = getRegionHeader(topLeft.chunkToRegion());
			} else {
				mcaFileTopLeft = mcaFileLeft;
			}
		}

		// check if this chunk is at the right-most border of this region
		if (relative.getX() == 31) {
			Point2i right = location.add(1, 0);
			mcaFileRight = getRegionHeader(right.chunkToRegion());

			if (relative.getZ() == 31) {
				Point2i bottomRight = location.add(1, 1);
				mcaFileBottomRight = getRegionHeader(bottomRight.chunkToRegion());
			} else {
				mcaFileBottomRight = mcaFileRight;
			}
		}

		// check if this chunk is at the top-most border of this region
		if (relative.getZ() == 0) {
			Point2i top = location.add(0, -1);
			mcaFileTop = getRegionHeader(top.chunkToRegion());

			if (relative.getX() == 31) {
				Point2i topRight = location.add(1, -1);
				mcaFileTopRight = getRegionHeader(topRight.chunkToRegion());
			} else {
				mcaFileTopRight = mcaFileTop;
			}
		} else {
			mcaFileTop = self;
			if (relative.getX() != 31) {
				mcaFileTopRight = self;
			} else {
				mcaFileTopRight = mcaFileRight;
			}
		}

		// check if this chunk is at the bottom-most border of this region
		if (relative.getZ() == 31) {
			Point2i bottom = location.add(0, 1);
			mcaFileBottom = getRegionHeader(bottom.chunkToRegion());

			if (relative.getX() == 0) {
				Point2i bottomLeft = location.add(-1, 1);
				mcaFileBottomLeft = getRegionHeader(bottomLeft.chunkToRegion());
			} else {
				mcaFileBottomLeft = mcaFileBottom;
			}
		} else {
			mcaFileBottom = self;
			if (relative.getX() != 0) {
				mcaFileBottomLeft = self;
			} else {
				mcaFileBottomLeft = mcaFileLeft;
			}
		}

		if (relative.getX() != 0){
			mcaFileLeft = self;
			if (relative.getZ() != 0) {
				mcaFileTopLeft = self;
			} else {
				mcaFileTopLeft = mcaFileTop;
			}
		}

		if (relative.getX() != 31) {
			mcaFileRight = self;
			if (relative.getZ() != 31) {
				mcaFileBottomRight = self;
			} else {
				mcaFileBottomRight = mcaFileBottom;
			}
		}

		if (mcaFileTop == null || !mcaFileTop.hasChunkIndex(location.add(0, -1))) {
			count++;
		}
		if (mcaFileRight == null || !mcaFileRight.hasChunkIndex(location.add(1, 0))) {
			count++;
		}
		if (mcaFileBottom == null || !mcaFileBottom.hasChunkIndex(location.add(0, 1))) {
			count++;
		}
		if (mcaFileLeft == null || !mcaFileLeft.hasChunkIndex(location.add(-1, 0))) {
			count++;
		}

		if (mcaFileTopLeft == null || !mcaFileTopLeft.hasChunkIndex(location.add(-1, -1))) {
			count++;
		}
		if (mcaFileTopRight == null || !mcaFileTopRight.hasChunkIndex(location.add(1, -1))) {
			count++;
		}
		if (mcaFileBottomRight == null || !mcaFileBottomRight.hasChunkIndex(location.add(1, 1))) {
			count++;
		}
		if (mcaFileBottomLeft == null || !mcaFileBottomLeft.hasChunkIndex(location.add(-1, 1))) {
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
			if (!regionMCAFile.getFile().exists() || regionMCAFile.getFile().length() <= FileHelper.HEADER_SIZE) {
				push(key, null);
				return null;
			}
			byte[] data = new byte[(int) regionMCAFile.getFile().length()];
			try (InputStream is = Files.newInputStream(regionMCAFile.getFile().toPath(), StandardOpenOption.READ)) {
				is.read(data);
				regionMCAFile.loadBorderChunks(new ByteArrayPointer(data));
			} catch (IOException ex) {
				LOGGER.warn("failed to read data from {}", regionMCAFile.getFile(), ex);
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
