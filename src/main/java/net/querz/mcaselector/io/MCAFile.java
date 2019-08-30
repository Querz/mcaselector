package net.querz.mcaselector.io;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.filter.FilterData;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import java.io.*;
import java.util.*;

public class MCAFile {

	public static final int INDEX_HEADER_LOCATION = 0;
	public static final int TIMESTAMP_HEADER_LOCATION = 4096;
	public static final int SECTION_SIZE = 4096;

	private Point2i location;

	private File file;
	private int[] offsets;
	private byte[] sectors;
	private int[] timestamps;

	private MCAChunkData[] chunks;

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

				int sectors = (lastWritten >> 12) + 1;

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

	public static MCAFile readAll(File file, ByteArrayPointer ptr) {
		MCAFile m = readHeader(file, ptr);
		if (m != null) {
			for (int i = 0; i < m.offsets.length; i++) {
				m.chunks[i] = m.getChunkData(i);
				try {
					m.chunks[i].readHeader(ptr);
					m.chunks[i].loadData(ptr);
				} catch (Exception ex) {
					Debug.errorf("failed to load chunk at index %d: %s", i, ex.getMessage());
					ex.printStackTrace();
				}
			}
		}
		return m;
	}

	public static MCAFile readHeader(File file, ByteArrayPointer ptr) {
		try {
			MCAFile mcaFile = new MCAFile(file);
			mcaFile.readHeader(ptr);
			return mcaFile;
		} catch (Exception ex) {
			Debug.error(ex);
		}
		return null;
	}

	public void readHeader(ByteArrayPointer ptr) throws IOException {
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

	public void deleteChunkIndices(Filter filter, Set<Point2i> selection) {
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz  * Tile.SIZE_IN_CHUNKS + cx;

				MCAChunkData data = chunks[index];

				if (data == null || data.isEmpty() || selection != null && !selection.contains(data.getLocation())) {
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

	public void keepChunkIndices(Filter filter, Set<Point2i> selection) {
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz * Tile.SIZE_IN_CHUNKS + cx;

				MCAChunkData data = chunks[index];

				if (data == null || data.isEmpty()) {
					continue;
				}

				FilterData filterData = new FilterData(data.getTimestamp(), data.getData());

				//keep chunk if filter AND selection applies
				//ignore selection if it's null
				if (!filter.matches(filterData) || selection != null && !selection.contains(data.getLocation())) {
					offsets[index] = 0;
					sectors[index] = 0;
					timestamps[index] = 0;
				}
			}
		}
	}

	public Set<Point2i> getFilteredChunks(Filter filter) {
		Set<Point2i> chunks = new HashSet<>();
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz * Tile.SIZE_IN_CHUNKS + cx;

				MCAChunkData data = this.chunks[index];

				if (data == null || data.isEmpty()) {
					continue;
				}

				FilterData filterData = new FilterData(data.getTimestamp(), data.getData());

				if (filter.matches(filterData)) {
					Point2i location = data.getLocation();
					if (location == null) {
						continue;
					}
					chunks.add(location);
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
				if (selection == null || selection.contains(chunk.getLocation())) {
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

	public void mergeChunksInto(MCAFile destination, Point2i offset, boolean overwrite) {
		int startX = offset.getX() > 0 ? 0 : Tile.SIZE_IN_CHUNKS - (Tile.SIZE_IN_CHUNKS + offset.getX());
		int limitX = offset.getX() > 0 ? (Tile.SIZE_IN_CHUNKS - offset.getX()) : Tile.SIZE_IN_CHUNKS;
		int startZ = offset.getY() > 0 ? 0 : Tile.SIZE_IN_CHUNKS - (Tile.SIZE_IN_CHUNKS + offset.getY());
		int limitZ = offset.getY() > 0 ? (Tile.SIZE_IN_CHUNKS - offset.getY()) : Tile.SIZE_IN_CHUNKS;

		for (int x = startX; x < limitX; x++) {
			for (int z = startZ; z < limitZ; z++) {
				int sourceIndex = z * Tile.SIZE_IN_CHUNKS + x;
				int destX = offset.getX() > 0 ? offset.getX() + x : x - startX;
				int destZ = offset.getY() > 0 ? offset.getY() + z : z - startZ;
				int destIndex = destZ * Tile.SIZE_IN_CHUNKS + destX;

				MCAChunkData sourceChunk = chunks[sourceIndex];
				MCAChunkData destinationChunk = destination.chunks[destIndex];

				if (!overwrite && destinationChunk != null && !destinationChunk.isEmpty()) {
					continue;
				}

				if (sourceChunk != null && !sourceChunk.isEmpty()) {
					if (!sourceChunk.applyOffset(offset.chunkToBlock())) {
						continue;
					}
					destination.chunks[destIndex] = sourceChunk;
				}
			}
		}
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

	public MCAChunkData getChunkData(int index) {
		return new MCAChunkData(offsets[index], timestamps[index], sectors[index]);
	}

	public File getFile() {
		return file;
	}

	public Image createImage(ByteArrayPointer ptr) {
		try {
			WritableImage finalImage = new WritableImage(Tile.SIZE, Tile.SIZE);
			PixelWriter writer = finalImage.getPixelWriter();

			for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
				for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
					int index = cz  * Tile.SIZE_IN_CHUNKS + cx;

					MCAChunkData data = getChunkData(index);

					data.readHeader(ptr);

					try {
						data.loadData(ptr);
					} catch (Exception ex) {
						Debug.error(ex);
					}

					data.drawImage(cx * Tile.CHUNK_SIZE, cz * Tile.CHUNK_SIZE, writer);
				}
			}
			return finalImage;
		} catch (Exception ex) {
			Debug.error(ex);
		}
		return null;
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
