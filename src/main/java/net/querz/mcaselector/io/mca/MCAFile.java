package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.selection.ChunkSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class MCAFile<T extends Chunk> implements Cloneable {

	private static final Logger LOGGER = LogManager.getLogger(MCAFile.class);

	protected Point2i location;

	protected File file;
	protected int[] timestamps;

	protected T[] chunks;

	private transient int[] offsets;
	private transient byte[] sectors;

	protected Function<Point2i, T> chunkConstructor;

	// file name must have well formed mca file format (r.<x>.<z>.mca)
	public MCAFile(File file, Function<Point2i, T> chunkConstructor) {
		Point2i location = FileHelper.parseMCAFileName(file);
		if (location == null) {
			throw new IllegalArgumentException("failed to parse region file name from " + file);
		}
		this.location = location;
		this.file = file;
		this.timestamps = new int[1024];
		this.chunkConstructor = chunkConstructor;
	}

	protected MCAFile(Point2i location) {
		this.location = location;
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
				LOGGER.debug("deleted empty region file {}", dest);
			} else {
				LOGGER.warn("failed to delete empty region file {}", dest);
			}

			if (!tempFile.delete()) {
				LOGGER.warn("failed to delete temp file {}", tempFile);
			}
		} else {
			Files.move(tempFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return result;
	}

	public boolean save(RandomAccessFile raf) throws IOException {
		int globalOffset = 2;
		int lastWritten = 0;

		raf.seek(0);
		for (int i = 0; i < 1024; i++) {
			raf.seek(globalOffset * 4096L);
			T chunk = chunks[i];

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
			raf.writeInt(chunk.getTimestamp());

			globalOffset += sectors;
		}

		// padding
		if (lastWritten % 4096 != 0) {
			raf.seek(globalOffset * 4096L - 1);
			raf.write(0);
		}

		return globalOffset != 2;
	}

	public void deFragment() throws IOException {
		deFragment(file);
	}

	// reads raw chunk data from source and writes it into a new temp file,
	// depending on which chunks of this MCA file are present in memory.
	public void deFragment(File dest) throws IOException {
		// loadHeader needs to be called before, otherwise this will delete everything

		// create temp file
		File tmpFile = File.createTempFile(file.getName(), null, null);
		int globalOffset = 2; // chunk data starts at 8192 (after 2 sectors)

		int skippedChunks = 0;

		// rafTmp if on the new file
		try (RandomAccessFile rafTmp = new RandomAccessFile(tmpFile, "rw");
		     RandomAccessFile source = new RandomAccessFile(file, "r")) {
			// loop over all offsets, readHeader the raw byte data (complete sections) and write it to new file
			for (int i = 0; i < offsets.length; i++) {
				// don't do anything if this chunk is empty
				if (offsets[i] == 0 || sectors[i] <= 0) {
					skippedChunks++;
					continue;
				}

				int sectors = this.sectors[i];

				// write offset and sector size to tmp file
				rafTmp.seek(i * 4L);
				rafTmp.writeByte(globalOffset >>> 16);
				rafTmp.writeByte(globalOffset >> 8 & 0xFF);
				rafTmp.writeByte(globalOffset & 0xFF);
				rafTmp.writeByte(sectors);

				// write timestamp to tmp file
				rafTmp.seek(4096 + i * 4L);
				rafTmp.writeInt(timestamps[i]);

				// copy chunk data to tmp file
				source.seek(offsets[i] * 4096L);
				rafTmp.seek(globalOffset * 4096L);

				DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(source.getFD()), sectors * 4096));
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rafTmp.getFD()), sectors * 4096));

				byte[] data = new byte[sectors * 4096];
				dis.read(data);
				dos.write(data);
				offsets[i] = globalOffset; // always keep MCAFile information up to date
				globalOffset += sectors;
			}
		}

		if (skippedChunks == 1024) {
			LOGGER.debug("all chunks in {} deleted, removing entire file", file.getAbsolutePath());
			if (tmpFile.exists() && !tmpFile.delete()) {
				LOGGER.warn("failed to delete tmpFile {} after all chunks were deleted", tmpFile.getAbsolutePath());
			}

			// only delete dest file if we are deFragmenting inside the source directory
			if (dest.getCanonicalPath().equals(file.getCanonicalPath())) {
				if (!dest.delete()) {
					LOGGER.warn("failed to delete file {} after all chunks were deleted", dest.getAbsolutePath());
				}
			}
		} else {
			LOGGER.debug("moving temp file {} to {}", tmpFile.getAbsolutePath(), dest.getAbsolutePath());
			Files.move(tmpFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public int[] load() throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			loadHeader(raf);

			Point2i origin = location.regionToChunk();

			for (short i = 0; i < 1024; i++) {
				if (offsets[i] == 0) {
					chunks[i] = null;
					continue;
				}
				raf.seek(offsets[i] * 4096L);

				Point2i chunkLocation = origin.add(new Point2i(i));

				try {
					chunks[i] = chunkConstructor.apply(chunkLocation);
					chunks[i].setTimestamp(timestamps[i]);
					chunks[i].load(raf);
				} catch (Exception ex) {
					chunks[i] = null;
					LOGGER.warn("failed to load chunk at {}", chunkLocation, ex);
				}
			}
			return offsets;
		}
	}

	public int[] load(ByteArrayPointer ptr) throws IOException {
		loadHeader(ptr);

		Point2i origin = location.regionToChunk();

		for (short i = 0; i < 1024; i++) {
			if (offsets[i] == 0) {
				chunks[i] = null;
				continue;
			}
			ptr.seek(offsets[i] * 4096L);

			Point2i chunkLocation = origin.add(new Point2i(i));

			try {
				chunks[i] = chunkConstructor.apply(chunkLocation);
				chunks[i].setTimestamp(timestamps[i]);
				chunks[i].load(ptr);
			} catch (Exception ex) {
				chunks[i] = null;
				LOGGER.debug("failed to load chunk at {}", chunkLocation, ex);
			}
		}
		return offsets;
	}

	public void loadHeader(RandomAccessFile raf) throws IOException {
		offsets = new int[1024];
		sectors = new byte[1024];

		raf.seek(0);
		for (int i = 0; i < offsets.length; i++) {
			int offset = (raf.read()) << 16;
			offset |= (raf.read() & 0xFF) << 8;
			offsets[i] = offset | raf.read() & 0xFF;
			sectors[i] = raf.readByte();
		}

		// read timestamps
		for (int i = 0; i < 1024; i++) {
			timestamps[i] = raf.readInt();
		}
	}

	public void loadHeader(ByteArrayPointer ptr) throws IOException {
		offsets = new int[1024];
		sectors = new byte[1024];

		try {
			ptr.seek(0);
			for (int i = 0; i < offsets.length; i++) {
				int offset = (ptr.read()) << 16;
				offset |= (ptr.read() & 0xFF) << 8;
				offsets[i] = offset | ptr.read() & 0xFF;
				sectors[i] = ptr.readByte();
			}

			// read timestamps
			timestamps = new int[1024];
			for (int i = 0; i < 1024; i++) {
				timestamps[i] = ptr.readInt();
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new IOException(ex);
		}
	}

	public T loadSingleChunk(Point2i chunk) throws IOException {
		// ignore files that don't have a full header
		if (file.length() < FileHelper.HEADER_SIZE) {
			return null;
		}

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

			Point2i absoluteChunkLocation = region.regionToChunk().add(rel);

			// read timestamp
			raf.seek(headerOffset + 4096L);
			int timestamp = raf.readInt();

			// read chunk data
			T chunkData = chunkConstructor.apply(absoluteChunkLocation);
			chunkData.setTimestamp(timestamp);

			if (offset > 0) {
				raf.seek(offset * 4096L);
				chunkData.load(raf);
			}

			return chunkData;
		}
	}

	public void loadBorderChunks(ByteArrayPointer ptr) throws IOException {
		loadHeader(ptr);

		// top row / bottom row
		for (int x = 0; x < 32; x++) {
			loadChunk(ptr, x);
			loadChunk(ptr, x + 992);
		}

		// left row / right row
		for (int z = 1; z < 31; z++) {
			loadChunk(ptr, z * 32);
			loadChunk(ptr, 31 + z * 32);
		}
	}

	private void loadChunk(ByteArrayPointer ptr, int index) throws IOException {
		try {
			if (offsets[index] == 0) {
				chunks[index] = null;
				return;
			}
			ptr.seek(offsets[index] * 4096L);

			Point2i origin = location.regionToChunk();

			Point2i chunkLocation = origin.add(new Point2i(index));

			try {
				chunks[index] = chunkConstructor.apply(chunkLocation);
				chunks[index].setTimestamp(timestamps[index]);
				chunks[index].load(ptr);
			} catch (Exception ex) {
				chunks[index] = null;
				LOGGER.warn("failed to load chunk at {}", chunkLocation, ex);
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new IOException(ex);
		}
	}

	public void saveSingleChunk(Point2i location, T chunk) throws IOException {
		if (file.exists() && file.length() > 0) {
			load();
		} else if (chunk == null || chunk.isEmpty()) {
			LOGGER.debug("nothing to save and no existing file found for chunk {}", location);
			return;
		}

		int index = location.asChunkIndex();
		setChunk(index, chunk);
		if (chunk != null) {
			setTimestamp(index, chunk.getTimestamp());
		}
		saveWithTempFile();
	}

// END OF IO STUFF -----------------------------------------------------------------------------------------------------

// DATA MANIPULATION STUFF ---------------------------------------------------------------------------------------------

	public void deleteChunks(ChunkSet selection) {
		for (int chunk : selection) {
			timestamps[chunk] = 0;
			chunks[chunk] = null;
			sectors[chunk] = 0;
			offsets[chunk] = 0;
		}
	}

	public abstract void mergeChunksInto(MCAFile<T> destination, Point3i offset, boolean overwrite, ChunkSet sourceChunks, ChunkSet targetChunks, List<Range> ranges);

	protected void mergeChunksInto(MCAFile<T> destination, Point3i offset, boolean overwrite, ChunkSet sourceChunks, ChunkSet targetChunks, List<Range> ranges, BiFunction<Point2i, Integer, T> chunkCreator) {
		Point2i relativeOffset = location.regionToChunk().add(offset.toPoint2i()).sub(destination.location.regionToChunk());
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

				T sourceChunk = chunks[sourceIndex];
				T destinationChunk = destination.chunks[destIndex];

				if (!overwrite && destinationChunk != null && !destinationChunk.isEmpty()) {
					continue;
				}

				if (sourceChunk == null || sourceChunk.isEmpty() || sourceChunks != null && !sourceChunks.get(sourceIndex)) {
					continue;
				}

				Point2i destChunk = destination.location.regionToChunk().add(destX, destZ);

				if (targetChunks == null || targetChunks.get(destIndex)) {
					if (!sourceChunk.relocate(offset.sectionToBlock())) {
						continue;
					}

					if (ranges != null) {
						int sourceVersion = sourceChunk.getDataVersion();
						if (sourceVersion == 0) {
							continue;
						}

						int destinationVersion;
						if (destinationChunk == null || destinationChunk.isEmpty()) {
							destinationChunk = chunkCreator.apply(destChunk, sourceVersion);
							destination.chunks[destIndex] = destinationChunk;
						} else if (sourceVersion != (destinationVersion = destinationChunk.getDataVersion())) {
							Point2i srcChunk = location.regionToChunk().add(x, z);
							LOGGER.warn("failed to merge chunk at {} into chunk at {} because their DataVersion does not match ({} != {})",
									srcChunk, destChunk, sourceVersion, destinationVersion);
						}

						try {
							sourceChunk.merge(destinationChunk.getData(), ranges, offset.getY());
						} catch (Exception ex) {
							Point2i srcChunk = location.regionToChunk().add(x, z);
							LOGGER.warn("failed to merge chunk {} into {}", srcChunk, destChunk, ex);
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

	public T getChunkAt(Point2i location) {
		return chunks[getChunkIndex(location)];
	}

	public T getChunk(int index) {
		return chunks[index];
	}

	public boolean hasChunkIndex(Point2i location) {
		return offsets[getChunkIndex(location)] != 0;
	}

	public void setChunkAt(Point2i location, T chunk) {
		chunks[getChunkIndex(location)] = chunk;
	}

	public void setChunk(int index, T chunk) {
		chunks[index] = chunk;
	}

	public void deleteChunk(int index) {
		chunks[index] = null;
		timestamps[index] = 0;
		offsets[index] = 0;
		sectors[index] = 0;
	}

	public boolean isEmpty() {
		for (T chunk : chunks) {
			if (chunk != null) {
				return false;
			}
		}
		return true;
	}

	protected <V extends MCAFile<T>> V clone(Function<File, V> mcaFileConstructor) {
		V clone = mcaFileConstructor.apply(file);
		for (int i = 0; i < chunks.length; i++) {
			if (chunks[i] != null) {
				clone.chunks[i] = chunks[i].clone(clone.chunkConstructor);
			}
		}
		clone.timestamps = timestamps.clone();
		return clone;
	}

	public abstract MCAFile<T> clone();
}
