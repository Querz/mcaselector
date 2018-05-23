package net.querz.mcaselector.io;

import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Point2i;
import java.awt.image.BufferedImage;
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
		this.file = file;
		offsets = new int[Tile.CHUNKS];
		sectors = new byte[Tile.CHUNKS];
		timestamps = new int[Tile.CHUNKS];
	}

	public void read(RandomAccessFile raf) throws IOException {
		raf.seek(0);
		for (int i = 0; i < offsets.length; i++) {
			int offset = raf.readByte() & 0xFF;
			offset <<= 8;
			offset |= raf.readByte() & 0xFF;
			offset <<= 8;
			offset |= raf.readByte() & 0xFF;
			offsets[i] = offset;
			sectors[i] = raf.readByte();
		}
		for (int i = 0; i < timestamps.length; i++) {
			timestamps[i] = raf.readInt();
		}
	}

	//chunks contains chunk coordinates to be deleted in this file.
	public void deleteChunkIndices(Set<Point2i> chunks, RandomAccessFile raf) throws Exception {
		for (Point2i chunk : chunks) {
			int index = getChunkIndex(chunk);
			raf.seek(INDEX_HEADER_LOCATION + index * 4);
			raf.writeInt(0);
			//set timestamp to 0
			raf.seek(TIMESTAMP_HEADER_LOCATION + index * 4);
			raf.writeInt(0);
		}
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

	public BufferedImage createImage(RandomAccessFile raf) {
		try {
			BufferedImage finalImage = new BufferedImage(Tile.SIZE, Tile.SIZE, BufferedImage.TYPE_INT_RGB);

			for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
				for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
					int index = cz  * Tile.SIZE_IN_CHUNKS + cx;
					MCAChunkData data = getChunkData(index);
					data.readHeader(raf);
					try {
						data.loadData(raf);
					} catch (Exception ex) {
						Debug.error(ex);
					}

					int imageX = cx * Tile.CHUNK_SIZE;
					int imageZ = cz * Tile.CHUNK_SIZE;

					data.drawImage(imageX, imageZ, finalImage);
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
