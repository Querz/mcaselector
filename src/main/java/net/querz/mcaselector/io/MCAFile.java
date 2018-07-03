package net.querz.mcaselector.io;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.filter.structure.Filter;
import net.querz.mcaselector.filter.structure.FilterData;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Point2i;
import java.io.*;
import java.util.Set;

public class MCAFile {

	public static final int INDEX_HEADER_LOCATION = 0;
	public static final int TIMESTAMP_HEADER_LOCATION = 4096;
	public static final int SECTION_SIZE = 4096;

	private File file;
	private int[] offsets;
	private byte[] sectors;
	private int[] timestamps;

	public MCAFile(File file) {
		this.file = file.getAbsoluteFile();
		offsets = new int[Tile.CHUNKS];
		sectors = new byte[Tile.CHUNKS];
		timestamps = new int[Tile.CHUNKS];
	}

	public void read(RandomAccessFile raf) throws IOException {
		raf.seek(0);
		//use streams wherever possible, because they can be buffered
		DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(raf.getFD()), SECTION_SIZE * 2));
		for (int i = 0; i < offsets.length; i++) {
			int offset = dis.readByte() & 0xFF;
			offset <<= 8;
			offset |= dis.readByte() & 0xFF;
			offset <<= 8;
			offset |= dis.readByte() & 0xFF;
			offsets[i] = offset;
			sectors[i] = dis.readByte();
		}
		for (int i = 0; i < timestamps.length; i++) {
			timestamps[i] = dis.readInt();
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

	public void deleteChunkIndices(Filter filter, RandomAccessFile raf) throws Exception {
		for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
			for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
				int index = cz  * Tile.SIZE_IN_CHUNKS + cx;

				MCAChunkData data = getChunkData(index);
				data.readHeader(raf);

				if (data.isEmpty()) {
					continue;
				}

				try {
					data.loadData(raf);
				} catch (Exception ex) {
					Debug.error(ex);
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

	//will rearrange the chunk data in the mca file to take up as few space as possible
	public File deFragment(RandomAccessFile raf) throws Exception {
		//only works if read has been called before

		File tmpFile = File.createTempFile(file.getName(), null, null);
		int globalOffset = 2; //chunk data starts at 8192 (after 2 sectors)

		int skippedChunks = 0;

		//rafTmp if on the new file
		try (RandomAccessFile rafTmp = new RandomAccessFile(tmpFile, "rw")) {
			//loop over all offsets, read the raw byte data (complete sections) and write it to new file
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
				Debug.dump("could not delete tmpFile " + tmpFile + " after all chunks were deleted");
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

	public Image createImage(RandomAccessFile raf) {
		try {
			WritableImage finalImage = new WritableImage(Tile.SIZE, Tile.SIZE);
			PixelWriter writer = finalImage.getPixelWriter();

			long start = System.currentTimeMillis();
			long loadingTime = 0;
			for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
				for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
					int index = cz  * Tile.SIZE_IN_CHUNKS + cx;

					long s = System.currentTimeMillis();
					MCAChunkData data = getChunkData(index);
					data.readHeader(raf);
					try {
						data.loadData(raf);
					} catch (Exception ex) {
						Debug.error(ex);
					}
					loadingTime += (System.currentTimeMillis() - s);

					int imageX = cx * Tile.CHUNK_SIZE;
					int imageZ = cz * Tile.CHUNK_SIZE;

					data.drawImage(imageX, imageZ, writer);
				}
			}
			Debug.dump("took " + (System.currentTimeMillis() - start)
					+ "ms to generate image of region, loading time: " + loadingTime + "ms");
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
