package net.querz.mcaselector.io;

import net.querz.mcaselector.ChunkDataProcessor;
import net.querz.mcaselector.ColorMapping;

import java.awt.image.BufferedImage;
import java.io.*;

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

	public void read(DataInputStream dis) throws IOException {
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

	public MCAChunkData getChunkData(int index) {
		return new MCAChunkData(offsets[index], timestamps[index], sectors[index]);
	}

	public File getFile() {
		return file;
	}

	public BufferedImage createImage(ChunkDataProcessor chunkDataProcessor, ColorMapping colorMapping) {
		try (
			RandomAccessFile raf = new RandomAccessFile(file, "r")
		) {
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

					data.drawImage(chunkDataProcessor, colorMapping, imageX, imageZ, finalImage);
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
