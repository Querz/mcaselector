package net.querz.mcaselector.io;

import net.querz.mcaselector.ChunkDataProcessor;
import net.querz.mcaselector.ColorMapping;
import net.querz.mcaselector.util.Point2i;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Set;

public class MCAFile {

	private File file;
	private int[] offsets;
	private byte[] sectors;
	private int[] timestamps;

	public MCAFile(File file) {
		this.file = file;
		offsets = new int[1024];
		sectors = new byte[1024];
		timestamps = new int[1024];
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
			System.out.println("deleting index " + index + " of " + chunk);
			raf.seek(index * 4);
			raf.writeInt(0);
			//set timestamp to 0
			raf.seek(4096 + index * 4);
			raf.writeInt(0);
		}
	}

	private int getChunkIndex(Point2i chunkCoordinate) {
		return (chunkCoordinate.getX() & 31) + (chunkCoordinate.getY() & 31) * 32;
	}

	public MCAChunkData getChunkData(int index) {
		return new MCAChunkData(offsets[index], timestamps[index], sectors[index]);
	}

	public File getFile() {
		return file;
	}

	public BufferedImage createImage(RandomAccessFile raf) {
		try {
			BufferedImage finalImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);

			for (int cx = 0; cx < 32; cx++) {
				for (int cz = 0; cz < 32; cz++) {
					int index = cz  * 32 + cx;
					MCAChunkData data = getChunkData(index);
					data.readHeader(raf);
					try {
						data.loadData(raf);
					} catch (Exception ex) {
						ex.printStackTrace();
					}

					int imageX = cx * 16;
					int imageZ = cz * 16;

					data.drawImage(imageX, imageZ, finalImage);
				}
			}
			return finalImage;
		} catch (Exception ex) {
			ex.printStackTrace();
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
