package net.querz.mcaselector.io;

import net.querz.mcaselector.ChunkDataProcessor;
import net.querz.mcaselector.ColorMapping;
import net.querz.nbt.CompoundTag;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

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

	/*
	* TODO:
	* try to only overwrite indices, sector counts and timestamps of deleted chunks with 0
	* without actually deleting the data.
	* look into whether mc will delete the data by itself at one point.
	* */
	//length of chunks is always 1024, if one chunk is deleted, it is null
	//each MCAChunkData needs rawChunkData
	public void write(MCAChunkData[] chunks, RandomAccessFile raf) throws Exception {
		if (chunks.length != 1024) {
			throw new IllegalArgumentException("chunks array must have a length of 1024");
		}
		int offset = 2;
//		(RandomAccessFile raf = new RandomAccessFile(file, "w"))
		for (int i = 0; i < chunks.length; i++) {
			System.out.println("offset in sectors: " + offset);
			MCAChunkData chunk = chunks[i];
			if (chunk == null) {
				//set offset and sector count to 0
				raf.seek(i * 4);
				raf.writeInt(0);
				//set timestamp to 0
				raf.seek(4096 + i * 4);
				raf.writeInt(0);

				continue;
			}

//			chunk.saveData(raf, offset * 4096);
//			//padding for each chunk is written automatically by skipping bytes
//			System.out.println("go to indices index " + i * 4);
//			raf.seek(i * 4);
//			//write offset
//			System.out.println("writing offset " + offset + " as offset << 8");
//			raf.writeInt(offset << 8);
//
//			System.out.println("going back by 1 byte");
//			raf.seek(raf.getFilePointer() - 1);
//			//write sector count
//			System.out.println("writing sectors " + chunk.getSectors());
//			raf.write(chunk.getSectors());
//
//			System.out.println("go to locations index " + (4096 + i * 4));
//			raf.seek(4096 + i * 4);
//			//write timestamp
//			System.out.println("writing timestamp " + chunk.getTimestamp());
//			raf.writeInt(chunk.getTimestamp());
//
//			//recalculate offset
//			offset += chunk.getSectors();
//
//			System.out.println("new offset " + offset);
		}

		//padding
		raf.write(new byte[4096 - (5 + chunks[1023].getLength()) % 4096]);
	}

	public MCAChunkData getChunkData(int index) {
		return new MCAChunkData(offsets[index], timestamps[index], sectors[index]);
	}

	public File getFile() {
		return file;
	}

	public BufferedImage createImage(ChunkDataProcessor chunkDataProcessor, ColorMapping colorMapping, RandomAccessFile raf) {
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
