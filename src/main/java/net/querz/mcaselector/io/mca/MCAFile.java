package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.util.exception.ThrowingConsumer;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.selection.ChunkSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class MCAFile<T extends Chunk> {

	private static final Logger LOGGER = LogManager.getLogger(MCAFile.class);

	protected Point2i location;

	protected File file;
	protected int[] timestamps;

	protected T[] chunks;

	private transient int[] offsets;
	private transient byte[] sectors;

	protected Function<Point2i, T> chunkConstructor;


	// initialize reflection for foreign API
	private static Method arenaOfSharedMethod = null;
	private static Method fileChannelMapMethod = null;
	private static Method memorySegmentAsByteBufferMethod = null;
	private static Method arenaCloseMethod = null;
	private static boolean useForeignAPI = false;

	static {
		try {
			Class<?> arenaClass = Class.forName("java.lang.foreign.Arena");
			Class<?> memorySegmentClass = Class.forName("java.lang.foreign.MemorySegment");
			arenaOfSharedMethod = arenaClass.getMethod("ofShared");
			fileChannelMapMethod = FileChannel.class.getMethod("map", FileChannel.MapMode.class, long.class, long.class, arenaClass);
			memorySegmentAsByteBufferMethod = memorySegmentClass.getMethod("asByteBuffer");
			arenaCloseMethod = arenaClass.getMethod("close");
			useForeignAPI = true;
			LOGGER.info("successfully initialized foreign API");
		} catch (ReflectiveOperationException e) {
			LOGGER.info("failed to initialize foreign API, falling back to HeapByteBuffer");
		}
	}

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
			if (dest.exists()) {
				if (dest.delete()) {
					LOGGER.debug("deleted empty region file {}", dest);
				} else {
					LOGGER.warn("failed to delete empty region file {}", dest);
				}
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
				if (offsets[i] == 0 || sectors[i] == 0) {
					skippedChunks++;
					continue;
				}

				int sectors = this.sectors[i] & 0xFF;

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
				byte[] data = new byte[sectors * 4096];
				source.readFully(data);
				rafTmp.write(data);

				// keep MCAFile information up to date
				offsets[i] = globalOffset;
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

	public void load(boolean raw) throws IOException {
		try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			if (fc.size() < 8196) {
				return;
			}
			Point2i origin = location.regionToChunk();

			loadBuffer(fc, (int) fc.size(), buf -> {
				loadHeader(buf);
				for (short i = 0; i < 1024; i++) {
					if (offsets[i] == 0) {
						chunks[i] = null;
						continue;
					}
					buf.position(offsets[i] * 4096);

					Point2i chunkLocation = origin.add(new Point2i(i));

					try {
						chunks[i] = chunkConstructor.apply(chunkLocation);
						chunks[i].setTimestamp(timestamps[i]);
						chunks[i].load(buf, raw);
					} catch (Exception ex) {
						chunks[i] = null;
						LOGGER.warn("failed to load chunk at {}", chunkLocation, ex);
					}
				}

			});
		}
	}

	public void loadHeader() throws IOException {
		try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			loadBuffer(fc, FileHelper.HEADER_SIZE, this::loadHeader);
		}
	}

	private void loadHeader(ByteBuffer buf) throws IOException {
		offsets = new int[1024];
		sectors = new byte[1024];

		try {
			buf.position(0);
			for (int i = 0; i < offsets.length; i++) {
				int offset = (buf.get()) << 16;
				offset |= (buf.get() & 0xFF) << 8;
				offsets[i] = offset | buf.get() & 0xFF;
				sectors[i] = buf.get();
			}

			// read timestamps
			timestamps = new int[1024];
			for (int i = 0; i < 1024; i++) {
				timestamps[i] = buf.getInt();
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

		Point2i region = FileHelper.parseMCAFileName(file);
		if (region == null) {
			throw new IOException("invalid region file name " + file);
		}

		Point2i rel = chunk.mod(32);
		rel.setX(rel.getX() < 0 ? 32 + rel.getX() : rel.getX());
		rel.setZ(rel.getZ() < 0 ? 32 + rel.getZ() : rel.getZ());
		int headerIndex = rel.getZ() * 32 + rel.getX();
		int headerOffset = headerIndex * 4;

		Point2i absoluteChunkLocation = region.regionToChunk().add(rel);
		T chunkData = chunkConstructor.apply(absoluteChunkLocation);

		try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			loadBuffer(fc, (int) fc.size(), buf -> {
				// read offset and timestamp for specific chunk
				buf.position(headerOffset);
				int offset = (buf.get()) << 16;
				offset |= (buf.get() & 0xFF) << 8;
				offset = offset | buf.get() & 0xFF;

				// read timestamp
				buf.position(headerOffset + 4096);
				int timestamp = buf.getInt();

				// read chunk data
				chunkData.setTimestamp(timestamp);

				if (offset > 0) {
					buf.position(offset * 4096);
					chunkData.load(buf, false);
				}
			});

			return chunkData;
		}
	}

	public void loadBorderChunks() throws IOException {
		try (FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			loadBuffer(fc, (int) fc.size(), buf -> {
				loadHeader(buf);

				// top row / bottom row
				for (int x = 0; x < 32; x++) {
					loadChunk(buf, x);
					loadChunk(buf, x + 992);
				}

				// left row / right row
				for (int z = 1; z < 31; z++) {
					loadChunk(buf, z * 32);
					loadChunk(buf, 31 + z * 32);
				}
			});
		}
	}

	private void loadChunk(ByteBuffer buf, int index) throws IOException {
		try {
			if (offsets[index] == 0) {
				chunks[index] = null;
				return;
			}
			buf.position(offsets[index] * 4096);

			Point2i origin = location.regionToChunk();

			Point2i chunkLocation = origin.add(new Point2i(index));

			try {
				chunks[index] = chunkConstructor.apply(chunkLocation);
				chunks[index].setTimestamp(timestamps[index]);
				chunks[index].load(buf, false);
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
			load(false);
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

	private void loadBuffer(FileChannel fc, int size, ThrowingConsumer<ByteBuffer, IOException> c) throws IOException {
		if (useForeignAPI) {
			Object arena = null;
			try {
				arena = arenaOfSharedMethod.invoke(null);
				Object memorySegment = fileChannelMapMethod.invoke(fc, FileChannel.MapMode.READ_ONLY, 0L, size, arena);
				ByteBuffer buf = (ByteBuffer) memorySegmentAsByteBufferMethod.invoke(memorySegment);
				c.accept(buf);
				return;
			} catch (ReflectiveOperationException e) {
				/* ignore */
			} finally {
				try {
					if (arena != null) {
						arenaCloseMethod.invoke(arena);
					}
				} catch (ReflectiveOperationException e) {
					LOGGER.error("failed to close arena", e);
				}
			}
		}
		// if something went wrong trying to use foreign API or if foreign API failed to initialize, fall back to HeapByteBuffer
		ByteBuffer buf = ByteBuffer.allocate(size);
		fc.read(buf);
		c.accept(buf);
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
					try {
						if (!sourceChunk.relocate(offset.sectionToBlock())) {
							continue;
						}
					} catch (Exception ex) {
						Point2i srcChunk = location.regionToChunk().add(x, z);
						LOGGER.warn("failed to relocate chunk {} to {}", srcChunk, destChunk, ex);
						continue;
					}

					if (ranges != null) {
						int sourceVersion = sourceChunk.getData().getIntOrDefault("DataVersion", 0);
						if (sourceVersion == 0) {
							continue;
						}

						int destinationVersion;
						if (destinationChunk == null || destinationChunk.isEmpty()) {
							destinationChunk = chunkCreator.apply(destChunk, sourceVersion);
							destination.chunks[destIndex] = destinationChunk;
						} else if (sourceVersion != (destinationVersion = destinationChunk.getData().getIntOrDefault("DataVersion", 0))) {
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
}
