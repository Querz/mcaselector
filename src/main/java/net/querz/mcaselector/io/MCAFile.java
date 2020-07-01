package net.querz.mcaselector.io;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.FilterData;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkDataProcessor;
import net.querz.mcaselector.version.VersionController;
import java.io.*;
import java.util.*;

public class MCAFile {

	public static final int INDEX_HEADER_LOCATION = 0;
	public static final int TIMESTAMP_HEADER_LOCATION = 4096;
	public static final int SECTION_SIZE = 4096;

	private final Point2i location;

	private final File file;
	private final int[] offsets;
	private final byte[] sectors;
	private final int[] timestamps;

	private final MCAChunkData[] chunks;

	public MCAFile(File file) {
		this.file = file.getAbsoluteFile();
		location = FileHelper.parseMCAFileName(file);
		if (location == null) {
			throw new IllegalArgumentException("invalid mca file name: " + file.getName());
		}
		offsets = new int[Tile.CHUNKS];
		sectors = new byte[Tile.CHUNKS];
		timestamps = new int[Tile.CHUNKS];
		chunks = new MCAChunkData[Tile.CHUNKS];
	}

	public boolean save(File file) {
		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			return saveAll(raf);
		} catch (Exception ex) {
			Debug.dumpException("failed to save MCAFile in " + file, ex);
		}
		return false;
	}

	//returns whether there were any chunks written to file
	public boolean saveAll(RandomAccessFile raf) throws Exception {
		int globalOffset = 2;
		int lastWritten = 0;

		raf.seek(0);
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz * Tile.SIZE_IN_CHUNKS + cx;

				raf.seek(globalOffset * SECTION_SIZE);
				MCAChunkData data = chunks[index];

				if (data == null || data.isEmpty()) {
					continue;
				}

				lastWritten = data.saveData(raf);

				int sectors = (lastWritten >> 12) + (lastWritten % SECTION_SIZE == 0 ? 0 : 1);

				raf.seek(INDEX_HEADER_LOCATION + index * 4);
				raf.writeByte(globalOffset >>> 16);
				raf.writeByte(globalOffset >> 8 & 0xFF);
				raf.writeByte(globalOffset & 0xFF);
				raf.writeByte(sectors);

				//write timestamp to tmp file
				raf.seek(TIMESTAMP_HEADER_LOCATION + index * 4);
				raf.writeInt(timestamps[index]);

				globalOffset += sectors;
			}
		}

		//padding
		if (lastWritten % SECTION_SIZE != 0) {
			raf.seek(globalOffset * SECTION_SIZE - 1);
			raf.write(0);
		}

		return globalOffset != 2;
	}

	public static MCAFile read(File file) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			MCAFile m = readHeader(file, raf);
			for (int i = 0; i < m.offsets.length; i++) {
				m.chunks[i] = m.getChunkData(i);
				try {
					m.chunks[i].readHeader(raf);
					m.chunks[i].loadData(raf);
				} catch (Exception ex) {
					Debug.dumpException("failed to load chunk at index " + i, ex);
				}
			}
			return m;
		}
	}

	public static MCAFile readAll(File file, ByteArrayPointer ptr) {
		MCAFile m = readHeader(file, ptr);
		if (m != null) {
			for (int i = 0; i < m.offsets.length; i++) {
				m.chunks[i] = m.getChunkData(i);
				try {
					m.chunks[i].readHeader(ptr);
					m.chunks[i].loadData(ptr);
				} catch (Exception ex) {
					Debug.dumpException("failed to load chunk at index " + i, ex);
				}
			}
		}
		return m;
	}

	// reads a single chunk from a region file as fast as possible
	public static MCAChunkData readSingleChunk(File file, Point2i chunk) {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			// read offset, sector count and timestamp for specific chunk

			Point2i region = FileHelper.parseMCAFileName(file);
			if (region == null) {
				throw new IOException("error parsing mca file name: " + file.getName());
			}

			Point2i rel = chunk.mod(32);
			rel.setX(rel.getX() < 0 ? 32 + rel.getX() : rel.getX());
			rel.setY(rel.getY() < 0 ? 32 + rel.getY() : rel.getY());
			int headerIndex = rel.getY() * 32 + rel.getX();
			int headerOffset = headerIndex * 4;

			raf.seek(headerOffset);
			int offset = (raf.read()) << 16;
			offset |= (raf.read() & 0xFF) << 8;
			offset = offset | raf.read() & 0xFF;
			byte sectors = raf.readByte();

			raf.seek(headerOffset + 4096);
			int timestamp = raf.readInt();

			// calculate absolute chunk location;
			Point2i absoluteChunkLocation = region.regionToChunk().add(rel);

			// read chunk data
			MCAChunkData chunkData = new MCAChunkData(absoluteChunkLocation, offset, timestamp, sectors);
			chunkData.readHeader(raf);
			chunkData.loadData(raf);

			return chunkData;
		} catch (IOException ex) {
			Debug.dumpException("error reading single chunk from" + file, ex);
			return null;
		}
	}

	public static MCAFile readHeader(File file, ByteArrayPointer ptr) {
		try {
			MCAFile mcaFile = new MCAFile(file);
			mcaFile.readHeader(ptr);
			return mcaFile;
		} catch (Exception ex) {
			Debug.dumpException("failed to read header of MCAFile from " + file, ex);
		}
		return null;
	}

	public static MCAFile readHeader(File file, RandomAccessFile raf) throws IOException {
		MCAFile mcaFile = new MCAFile(file);
		mcaFile.readHeader(raf);
		return mcaFile;
	}

	private void readHeader(RandomAccessFile raf) throws IOException {
		raf.seek(0);
		for (int i = 0; i < offsets.length; i++) {
			int offset = (raf.read()) << 16;
			offset |= (raf.read() & 0xFF) << 8;
			offsets[i] = offset | raf.read() & 0xFF;
			sectors[i] = raf.readByte();
		}
		for (int i = 0; i < timestamps.length; i++) {
			timestamps[i] = raf.readInt();
		}
	}

	private void readHeader(ByteArrayPointer ptr) throws IOException {
		ptr.seek(0);
		for (int i = 0; i < offsets.length; i++) {
			int offset = (ptr.read()) << 16;
			offset |= (ptr.read() & 0xFF) << 8;
			offsets[i] = offset | ptr.read() & 0xFF;
			sectors[i] = ptr.readByte();
		}
		for (int i = 0; i < timestamps.length; i++) {
			timestamps[i] = ptr.readInt();
		}
	}

	//chunks contains chunk coordinates to be deleted in this file.
	public void deleteChunkIndices(Set<Point2i> chunks) {
		for (Point2i chunk : chunks) {
			int index = getChunkIndex(chunk);
			offsets[index] = 0;
			sectors[index] = 0;
			timestamps[index] = 0;
		}
	}

	public void deleteChunkIndices(Filter<?> filter, Set<Point2i> selection) {
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz  * Tile.SIZE_IN_CHUNKS + cx;

				MCAChunkData data = chunks[index];

				if (data.isEmpty() || selection != null && !selection.contains(data.getAbsoluteLocation())) {
					continue;
				}

				FilterData filterData = new FilterData(data.getTimestamp(), data.getData());

				if (filter.matches(filterData)) {
					offsets[index] = 0;
					sectors[index] = 0;
					timestamps[index] = 0;
				}
			}
		}
	}

	public void keepChunkIndices(Filter<?> filter, Set<Point2i> selection) {
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz * Tile.SIZE_IN_CHUNKS + cx;

				MCAChunkData data = chunks[index];

				if (data.isEmpty()) {
					continue;
				}

				FilterData filterData = new FilterData(data.getTimestamp(), data.getData());

				//keep chunk if filter AND selection applies
				//ignore selection if it's null
				if (!filter.matches(filterData) || selection != null && !selection.contains(data.getAbsoluteLocation())) {
					offsets[index] = 0;
					sectors[index] = 0;
					timestamps[index] = 0;
				}
			}
		}
	}

	public Set<Point2i> getFilteredChunks(Filter<?> filter) {
		Set<Point2i> chunks = new HashSet<>();
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz * Tile.SIZE_IN_CHUNKS + cx;

				MCAChunkData data = this.chunks[index];

				if (data.isEmpty()) {
					continue;
				}

				FilterData filterData = new FilterData(data.getTimestamp(), data.getData());

				try {
					if (filter.matches(filterData)) {
						Point2i location = data.getAbsoluteLocation();
						if (location == null) {
							continue;
						}
						chunks.add(location);
					}
				} catch (Exception ex) {
					Debug.dumpException(String.format("failed to select chunk %s in %s", new Point2i(cx, cz), getFile().getName()), ex);
				}
			}
		}
		return chunks;
	}

	public void applyFieldChanges(List<Field<?>> fields, boolean force, Set<Point2i> selection) {
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz * Tile.SIZE_IN_CHUNKS + cx;
				MCAChunkData chunk = chunks[index];
				if (selection == null || selection.contains(chunk.getAbsoluteLocation())) {
					if (chunk != null && !chunk.isEmpty()) {
						chunk.changeData(fields, force);
					}
				}
			}
		}
	}

	public void mergeChunksInto(MCAFile destination, boolean overwrite) {
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz * Tile.SIZE_IN_CHUNKS + cx;
				MCAChunkData sourceChunk = chunks[index];
				MCAChunkData destinationChunk = destination.chunks[index];

				if (!overwrite && destinationChunk != null && !destinationChunk.isEmpty()) {
					continue;
				}

				if (sourceChunk != null && !sourceChunk.isEmpty()) {
					destination.chunks[index] = sourceChunk;
				}
			}
		}
	}

	public void mergeChunksInto(MCAFile destination, Point2i offset, boolean overwrite, Set<Point2i> sourceChunks, Set<Point2i> selection, List<Range> ranges) {
		Point2i relativeOffset = getRelativeOffset(location, destination.location, offset);
		int startX = relativeOffset.getX() > 0 ? 0 : Tile.SIZE_IN_CHUNKS - (Tile.SIZE_IN_CHUNKS + relativeOffset.getX());
		int limitX = relativeOffset.getX() > 0 ? (Tile.SIZE_IN_CHUNKS - relativeOffset.getX()) : Tile.SIZE_IN_CHUNKS;
		int startZ = relativeOffset.getY() > 0 ? 0 : Tile.SIZE_IN_CHUNKS - (Tile.SIZE_IN_CHUNKS + relativeOffset.getY());
		int limitZ = relativeOffset.getY() > 0 ? (Tile.SIZE_IN_CHUNKS - relativeOffset.getY()) : Tile.SIZE_IN_CHUNKS;

		for (int x = startX; x < limitX; x++) {
			for (int z = startZ; z < limitZ; z++) {
				int sourceIndex = z * Tile.SIZE_IN_CHUNKS + x;
				int destX = relativeOffset.getX() > 0 ? relativeOffset.getX() + x : x - startX;
				int destZ = relativeOffset.getY() > 0 ? relativeOffset.getY() + z : z - startZ;
				int destIndex = destZ * Tile.SIZE_IN_CHUNKS + destX;

				MCAChunkData sourceChunk = chunks[sourceIndex];
				MCAChunkData destinationChunk = destination.chunks[destIndex];

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
								destinationChunk = MCAChunkData.newEmptyLevelMCAChunkData(destChunk, sourceVersion);
								destination.chunks[destIndex] = destinationChunk;
							} else if (sourceVersion != (destinationVersion = destinationChunk.getData().getInt("DataVersion"))) {
								Point2i srcChunk = location.regionToChunk().add(x, z);
								Debug.errorf("can't merge chunk at %s into chunk at %s because their DataVersion does not match (%d != %d)",
									srcChunk, destChunk, sourceVersion, destinationVersion);
							}

							ChunkDataProcessor p = VersionController.getChunkDataProcessor(sourceChunk.getData().getInt("DataVersion"));
							try {
								p.mergeChunks(sourceChunk.getData(), destinationChunk.getData(), ranges);
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

	private static Point2i getRelativeOffset(Point2i source, Point2i target, Point2i offset) {
		return source.regionToChunk().add(offset).sub(target.regionToChunk());
	}

	//will rearrange the chunk data in the mca file to take up as few space as possible
	//returns the tmp file
	public File deFragment(RandomAccessFile raf) throws Exception {
		//only works if readHeader has been called before

		File tmpFile = File.createTempFile(file.getName(), null, null);
		int globalOffset = 2; //chunk data starts at 8192 (after 2 sectors)

		int skippedChunks = 0;

		//rafTmp if on the new file
		try (RandomAccessFile rafTmp = new RandomAccessFile(tmpFile, "rw")) {
			//loop over all offsets, readHeader the raw byte data (complete sections) and write it to new file
			for (int i = 0; i < offsets.length; i++) {
				//don't do anything if this chunk is empty
				if (offsets[i] == 0) {
					skippedChunks++;
					continue;
				}

				int sectors = this.sectors[i];

				//write offset and sector size to tmp file
				rafTmp.seek(INDEX_HEADER_LOCATION + i * 4);
				rafTmp.writeByte(globalOffset >>> 16);
				rafTmp.writeByte(globalOffset >> 8 & 0xFF);
				rafTmp.writeByte(globalOffset & 0xFF);
				rafTmp.writeByte(sectors);

				//write timestamp to tmp file
				rafTmp.seek(TIMESTAMP_HEADER_LOCATION + i * 4);
				rafTmp.writeInt(timestamps[i]);

				//copy chunk data to tmp file
				raf.seek(offsets[i] * SECTION_SIZE);
				rafTmp.seek(globalOffset * SECTION_SIZE);

				DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(raf.getFD()), sectors * SECTION_SIZE));
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rafTmp.getFD()), sectors * SECTION_SIZE));

				byte[] data = new byte[sectors * SECTION_SIZE];
				int read = dis.read(data);

				if (read != sectors * SECTION_SIZE) {
					throw new RuntimeException("deFragment read less data from original file than expected: " + read + " instead of " + sectors * SECTION_SIZE);
				}

				dos.write(data);
				offsets[i] = globalOffset; //always keep MCAFile information up to date
				globalOffset += sectors;
			}
		}

		if (skippedChunks == Tile.CHUNKS && tmpFile.exists()) {
			if (tmpFile.delete()) {
				return null;
			} else {
				Debug.dumpf("could not delete tmpFile %s after all chunks were deleted", tmpFile.getAbsolutePath());
			}
		}

		return tmpFile;
	}

	private int getChunkIndex(Point2i chunkCoordinate) {
		return (chunkCoordinate.getX() & (Tile.SIZE_IN_CHUNKS - 1))
				+ (chunkCoordinate.getY() & (Tile.SIZE_IN_CHUNKS - 1)) * Tile.SIZE_IN_CHUNKS;
	}

	private Point2i getChunkOffsetFromIndex(int index) {
		int x = index % Tile.SIZE_IN_CHUNKS;
		int z = index / Tile.SIZE_IN_CHUNKS;
		return new Point2i(x, z);
	}

	public MCAChunkData getChunkData(Point2i location) {
		return getChunkData(getChunkIndex(location));
	}

	public MCAChunkData getChunkData(int index) {
		return new MCAChunkData(location.regionToChunk().add(getChunkOffsetFromIndex(index)), offsets[index], timestamps[index], sectors[index]);
	}

	public MCAChunkData getLoadedChunkData(Point2i location) {
		return chunks[getChunkIndex(location)];
	}

	public void setChunkData(int index, MCAChunkData chunk) {
		chunks[index] = chunk;
	}

	public void setChunkData(Point2i location, MCAChunkData chunk) {
		chunks[getChunkIndex(location)] = chunk;
	}

	public void setTimeStamp(int index, int timestamp) {
		timestamps[index] = timestamp;
	}

	public int getTimestamp(int index) {
		return timestamps[index];
	}

	public File getFile() {
		return file;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < offsets.length; i++) {
			sb.append(offsets[i]).append(" ").append(sectors[i]).append("; ");
		}
		sb.append("\n");
		for (int timestamp : timestamps) {
			sb.append(timestamp).append("; ");
		}
		return sb.toString();
	}
}
