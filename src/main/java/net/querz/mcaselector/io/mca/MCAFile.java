package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.VersionController;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;

public class MCAFile {

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

	public boolean save() throws IOException {
		return save(file);
	}

	public boolean save(File dest) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(dest, "rw")) {
			return save(raf);
		}
	}

	public boolean saveWithTempFile() throws IOException {
		return saveWithTempFile(file);
	}

	// returns false if no chunk was saved and the file only consists of the mca header
	public boolean saveWithTempFile(File dest) throws IOException {
		File tempFile = File.createTempFile(dest.getName(), null, null);
		boolean result;
		try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
			result = save(raf);
		}
		if (!result) {
			if (dest.delete()) {
				Debug.dumpf("deleted empty region file %s", dest);
			} else {
				Debug.dumpf("failed to delete empty region file %s", dest);
			}

			tempFile.delete();
		} else {
			Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return result;
	}

	public boolean save(RandomAccessFile raf) throws IOException {
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

	public int[] load() throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			int[] offsets = loadHeader(raf);

			chunks = new Chunk[1024];
			Point2i origin = location.regionToChunk();

			for (int i = 0; i < 1024; i++) {
				if (offsets[i] == 0) {
					continue;
				}
				raf.seek(offsets[i] * 4096);

				Point2i chunkLocation = origin.add(getChunkOffsetFromIndex(i));

				try {
					chunks[i] = new Chunk(chunkLocation);
					chunks[i].load(raf);
				} catch (Exception ex) {
					Debug.dumpException("failed to load chunk at " + chunkLocation, ex);
				}
			}
			return offsets;
		}
	}

	public int[] load(ByteArrayPointer ptr) throws IOException {
		int[] offsets = loadHeader(ptr);

		chunks = new Chunk[1024];
		Point2i origin = location.regionToChunk();

		for (int i = 0; i < 1024; i++) {
			if (offsets[i] == 0) {
				continue;
			}
			ptr.seek(offsets[i] * 4096);

			Point2i chunkLocation = origin.add(getChunkOffsetFromIndex(i));

			try {
				chunks[i] = new Chunk(chunkLocation);
				chunks[i].load(ptr);
			} catch (IOException ex) {
				Debug.dumpException("failed to load chunk at " + chunkLocation, ex);
			}
		}
		return offsets;
	}

	public int[] loadHeader(RandomAccessFile raf) throws IOException {
		int[] offsets = new int[1024];

		raf.seek(0);
		for (int i = 0; i < offsets.length; i++) {
			int offset = (raf.read()) << 16;
			offset |= (raf.read() & 0xFF) << 8;
			offsets[i] = offset | raf.read() & 0xFF;
			raf.read();
		}

		// read timestamps
		timestamps = new int[1024];
		for (int i = 0; i < 1024; i++) {
			timestamps[i] = raf.readInt();
		}

		return offsets;
	}

	public int[] loadHeader(ByteArrayPointer ptr) throws IOException {
		int[] offsets = new int[1024];

		try {
			ptr.seek(0);
			for (int i = 0; i < offsets.length; i++) {
				int offset = (ptr.read()) << 16;
				offset |= (ptr.read() & 0xFF) << 8;
				offsets[i] = offset | ptr.read() & 0xFF;
				ptr.read();
			}

			// read timestamps
			timestamps = new int[1024];
			for (int i = 0; i < 1024; i++) {
				timestamps[i] = ptr.readInt();
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new IOException(ex);
		}

		return offsets;
	}

	public static Chunk loadSingleChunk(File file, Point2i chunk) throws IOException {
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
		int startX = relativeOffset.getX() > 0 ? 0 : 32 - (32 + relativeOffset.getX());
		int limitX = relativeOffset.getX() > 0 ? (32 - relativeOffset.getX()) : 32;
		int startZ = relativeOffset.getZ() > 0 ? 0 : 32 - (32 + relativeOffset.getZ());
		int limitZ = relativeOffset.getZ() > 0 ? (32 - relativeOffset.getZ()) : 32;

		for (int x = startX; x < limitX; x++) {
			for (int z = startZ; z < limitZ; z++) {
				int sourceIndex = z * 32 + x;
				int destX = relativeOffset.getX() > 0 ? relativeOffset.getX() + x : x - startX;
				int destZ = relativeOffset.getZ() > 0 ? relativeOffset.getZ() + z : z - startZ;
				int destIndex = destZ * 32 + destX;

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
								Debug.errorf("failed to merge chunk at %s into chunk at %s because their DataVersion does not match (%d != %d)",
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

// END OF DATA MANIPULATION STUFF --------------------------------------------------------------------------------------

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

	public Chunk getChunkAt(Point2i location) {
		return chunks[getChunkIndex(location)];
	}

	public Chunk getChunk(int index) {
		return chunks[index];
	}

	public void setChunkAt(Point2i location, Chunk chunk) {
		chunks[getChunkIndex(location)] = chunk;
	}

	public void setChunk(int index, Chunk chunk) {
		chunks[index] = chunk;
	}
}
