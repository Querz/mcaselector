package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.FilterData;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAChunkData;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.VersionController;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class MCAFile {

	private Point2i location;

	private File file;
	private int[] timestamps;

	private Chunk[] chunks;

	// file name must have well formed mca file format (r.<x>.<z>.mca)
	public MCAFile(File file) {
		Point2i location = FileHelper.parseMCAFileName(file);
		if (location == null) {
			throw new IllegalArgumentException("failed to parse region file name from " + file);
		}
		this.location = location;
		this.file = file;
	}

// IO STUFF ------------------------------------------------------------------------------------------------------------

	public boolean save() throws Exception {
		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			return save(raf);
		}
	}

	public boolean save(RandomAccessFile raf) throws Exception {
		int globalOffset = 2;
		int lastWritten = 0;

		raf.seek(0);
		for (int i = 0; i < 1024; i++) {
			raf.seek(globalOffset * 4096);
			Chunk chunk = chunks[i];

			if (chunk == null || chunk.isEmpty()) {
				continue;
			}

			lastWritten = chunk.save(raf);

			int sectors = (lastWritten >> 12) + (lastWritten % 4096 == 0 ? 0 : 1);

			raf.seek(i * 4);
			raf.write(globalOffset >>> 16);
			raf.write(globalOffset >> 8 & 0xFF);
			raf.write(globalOffset & 0xFF);
			raf.write(sectors);

			// write timestamp
			raf.seek(4096 + i * 4);
			raf.writeInt(timestamps[i]);

			globalOffset += sectors;
		}

		// padding
		if (lastWritten % 4096 != 0) {
			raf.seek(globalOffset * 4096 - 1);
			raf.write(0);
		}

		return globalOffset != 2;
	}

	public void load() throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			loadHeader(raf);

			chunks = new Chunk[1024];
			Point2i origin = location.regionToChunk();

			for (int i = 0; i < 1024; i++) {
				Point2i chunkLocation = origin.add(getChunkOffsetFromIndex(i));

				try {
					chunks[i] = new Chunk(chunkLocation);
					chunks[i].load(raf);
				} catch (Exception ex) {
					Debug.dumpException("failed to load chunk at " + chunkLocation, ex);
				}
			}
		}
	}

	public void load(ByteArrayPointer ptr) throws IOException {
		loadHeader(ptr);

		chunks = new Chunk[1024];
		Point2i origin = location.regionToChunk();

		for (int i = 0; i < 1024; i++) {
			Point2i chunkLocation = origin.add(getChunkOffsetFromIndex(i));

			try {
				chunks[i] = new Chunk(chunkLocation);
				chunks[i].load(ptr);
			} catch (Exception ex) {
				Debug.dumpException("failed to load chunk at " + chunkLocation, ex);
			}
		}
	}

	public void loadHeader(RandomAccessFile raf) throws IOException {
		raf.seek(4096);

		// read timestamps
		timestamps = new int[1024];
		for (int i = 0; i < 1024; i++) {
			timestamps[i] = raf.readInt();
		}
	}

	public void loadHeader(ByteArrayPointer ptr) throws IOException {
		ptr.seek(4096);

		// read timestamps
		timestamps = new int[1024];
		for (int i = 0; i < 1024; i++) {
			timestamps[i] = ptr.readInt();
		}
	}

	public static Chunk readSingleChunk(File file, Point2i chunk) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			// read offset, sector count and timestamp for specific chunk

			Point2i region = FileHelper.parseMCAFileName(file);
			if (region == null) {
				throw new IOException("invalid region file name " + file);
			}

			Point2i rel = chunk.mod(32);
			rel.setX(rel.getX() < 0 ? 32 + rel.getX() : rel.getX());
			rel.setZ(rel.getZ() < 0 ? 32 + rel.getZ() : rel.getZ());
			int headerIndex = rel.getZ() * 32 + rel.getX();
			int headerOffset = headerIndex * 4;

			// read offset
			raf.seek(headerOffset);
			int offset = (raf.read()) << 16;
			offset |= (raf.read() & 0xFF) << 8;
			offset = offset | raf.read() & 0xFF;

			raf.seek(offset);

			Point2i absoluteChunkLocation = region.regionToChunk().add(rel);

			// read chunk data
			Chunk chunkData = new Chunk(absoluteChunkLocation);
			chunkData.load(raf);

			return chunkData;
		}
	}

// END OF IO STUFF -----------------------------------------------------------------------------------------------------

// DATA MANIPULATION STUFF ---------------------------------------------------------------------------------------------

	public void deleteChunks(Set<Point2i> selection) {
		for (Point2i chunk : selection) {
			int index = getChunkIndex(chunk);
			timestamps[index] = 0;
			chunks[index] = null;
		}
	}

	public void deleteChunks(Filter<?> filter, Set<Point2i> selection) {
		for (int i = 0; i < 1024; i++) {
			Chunk chunk = chunks[i];

			if (chunk == null || chunk.isEmpty() || selection != null && !selection.contains(chunk.getAbsoluteLocation())) {
				continue;
			}

			FilterData filterData = new FilterData(timestamps[i], chunk.getData());

			if (filter.matches(filterData)) {
				timestamps[i] = 0;
				chunks[i] = null;
			}
		}
	}

	public void keepChunks(Filter<?> filter, Set<Point2i> selection) {
		for (int i = 0; i < 1024; i++) {
			Chunk chunk = chunks[i];

			if (chunk == null || chunk.isEmpty()) {
				continue;
			}

			FilterData filterData = new FilterData(timestamps[i], chunk.getData());

			// keep chunk if filter AND selection applies
			// ignore selection if it's null
			if (!filter.matches(filterData) || selection != null && !selection.contains(chunk.getAbsoluteLocation())) {
				timestamps[i] = 0;
				chunks[i] = null;
			}
		}
	}

	public Set<Point2i> getFilteredChunks(Filter<?> filter) {
		Set<Point2i> chunks = new HashSet<>();

		for (int i = 0; i < 1024; i++) {
			Chunk chunk = this.chunks[i];

			if (chunk == null || chunk.isEmpty()) {
				continue;
			}

			FilterData filterData = new FilterData(timestamps[i], chunk.getData());

			Point2i location = chunk.getAbsoluteLocation();
			try {
				if (filter.matches(filterData)) {
					if (location == null) {
						continue;
					}
					chunks.add(location);
				}
			} catch (Exception ex) {
				Debug.dumpException(String.format("failed to select chunk %s in %s", location, file), ex);
			}
		}
		return chunks;
	}

	public void applyFieldChanges(List<Field<?>> fields, boolean force, Set<Point2i> selection) {
		for (int i = 0; i < 1024; i++) {
			Chunk chunk = chunks[i];
			if (selection == null || selection.contains(chunk.getAbsoluteLocation())) {
				if (chunk != null && !chunk.isEmpty()) {
					chunk.change(fields, force);
				}
			}
		}
	}

	public void mergeChunksInto(MCAFile destination, Point2i offset, boolean overwrite, Set<Point2i> sourceChunks, Set<Point2i> selection, List<Range> ranges) {
		Point2i relativeOffset = location.regionToChunk().add(offset).sub(destination.location.regionToChunk());
		int startX = relativeOffset.getX() > 0 ? 0 : Tile.SIZE_IN_CHUNKS - (Tile.SIZE_IN_CHUNKS + relativeOffset.getX());
		int limitX = relativeOffset.getX() > 0 ? (Tile.SIZE_IN_CHUNKS - relativeOffset.getX()) : Tile.SIZE_IN_CHUNKS;
		int startZ = relativeOffset.getZ() > 0 ? 0 : Tile.SIZE_IN_CHUNKS - (Tile.SIZE_IN_CHUNKS + relativeOffset.getZ());
		int limitZ = relativeOffset.getZ() > 0 ? (Tile.SIZE_IN_CHUNKS - relativeOffset.getZ()) : Tile.SIZE_IN_CHUNKS;

		for (int x = startX; x < limitX; x++) {
			for (int z = startZ; z < limitZ; z++) {
				int sourceIndex = z * Tile.SIZE_IN_CHUNKS + x;
				int destX = relativeOffset.getX() > 0 ? relativeOffset.getX() + x : x - startX;
				int destZ = relativeOffset.getZ() > 0 ? relativeOffset.getZ() + z : z - startZ;
				int destIndex = destZ * Tile.SIZE_IN_CHUNKS + destX;

				Chunk sourceChunk = chunks[sourceIndex];
				Chunk destinationChunk = destination.chunks[destIndex];

				if (!overwrite && destinationChunk != null && !destinationChunk.isEmpty()) {
					continue;
				}

				if (sourceChunk == null || sourceChunk.isEmpty() || sourceChunks != null && !sourceChunks.contains(sourceChunk.getAbsoluteLocation())) {
					continue;
				}

				Point2i destChunk = destination.location.regionToChunk().add(destX, destZ);

				if (selection == null || selection.contains(destChunk)) {
					if (!sourceChunk.relocate(offset.chunkToBlock())) {
						continue;
					}

					if (ranges != null) {
						int sourceVersion = sourceChunk.getData().getInt("DataVersion");
						if (sourceVersion != 0) {
							int destinationVersion;
							if (destinationChunk == null || destinationChunk.isEmpty()) {
								destinationChunk = Chunk.newEmptyLevelMCAChunkData(destChunk, sourceVersion);
								destination.chunks[destIndex] = destinationChunk;
							} else if (sourceVersion != (destinationVersion = destinationChunk.getData().getInt("DataVersion"))) {
								Point2i srcChunk = location.regionToChunk().add(x, z);
								Debug.errorf("can't merge chunk at %s into chunk at %s because their DataVersion does not match (%d != %d)",
										srcChunk, destChunk, sourceVersion, destinationVersion);
							}

							ChunkMerger m = VersionController.getChunkMerger(sourceChunk.getData().getInt("DataVersion"));
							try {
								m.mergeChunks(sourceChunk.getData(), destinationChunk.getData(), ranges);
							} catch (Exception ex) {
								Point2i srcChunk = location.regionToChunk().add(x, z);
								Debug.dump(new Exception("failed to merge chunk " + srcChunk + " into " + destChunk, ex));
							}
						}
					} else {
						destination.chunks[destIndex] = sourceChunk;
					}
				}
			}
		}
	}

	private int getChunkIndex(Point2i chunkCoordinate) {
		return (chunkCoordinate.getX() & 0x1F) + (chunkCoordinate.getZ() & 0x1F) * 32;
	}

	private Point2i getChunkOffsetFromIndex(int index) {
		return new Point2i(index % 32, index / 32);
	}

	public Point2i getLocation() {
		return location;
	}

	public void setFile(File file) {
		Point2i location = FileHelper.parseMCAFileName(file);
		if (this.location.equals(location)) {
			this.location = location;
			this.file = file;
		} else {
			throw new IllegalArgumentException("invalid file name change for region file from " + this.file + " to " + file);
		}
	}

	public File getFile() {
		return file;
	}

	public void setTimestamp(int index, int timestamp) {
		timestamps[index] = timestamp;
	}

	public int getTimestamp(int index) {
		return timestamps[index];
	}
}
