package net.querz.mcaselector.io;

import net.querz.mcaselector.ChunkDataProcessor;
import net.querz.mcaselector.ColorMapping;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTInputStream;
import net.querz.nbt.NBTOutputStream;
import net.querz.nbt.Tag;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class MCAChunkData {
	private long offset; //in actual bytes
	private int timestamp;
	private byte sectors;
	private int length; //length without padding
	private CompressionType compressionType;
	private CompoundTag data;
	private byte[] rawData;

	//offset in 4KiB chunks
	public MCAChunkData(int offset, int timestamp, byte sectors) {
		this.offset = ((long) offset) * 4096;
		this.timestamp = timestamp;
		this.sectors = sectors;
	}

	public boolean isEmpty() {
		return offset == 0 && timestamp == 0 && sectors == 0;
	}

	public void readHeader(RandomAccessFile raf) throws Exception {
		raf.seek(offset);
		length = raf.readInt();
		compressionType = CompressionType.fromByte(raf.readByte());
	}

	//use this for rewriting region files, doesn't should be faster
	public void loadRawData(RandomAccessFile raf) throws Exception {
		raf.seek(offset + 5);
		MCAInputStream in = new MCAInputStream(raf);
		rawData = new byte[length];

		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(in))) {
			int bytesRead = dis.read(rawData);
			if (bytesRead != length) {
				throw new RuntimeException("expected length does not match actual length of chunk data: " + length + " --> " + bytesRead);
			}
		}
	}

	//ignores offset, because it might have been changed
	//offset provided to this method is in bytes
	public void saveData(RandomAccessFile raf, long offset) throws Exception {
		if (rawData == null && data != null) {
			//if some idiot loaded the chunk data as NBT data instead of raw bytes
			NBTOutputStream nbtos = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				switch (compressionType) {
				case GZIP:
					nbtos = new NBTOutputStream(new GZIPOutputStream(baos));
					break;
				case ZLIB:
					nbtos = new NBTOutputStream(new DeflaterOutputStream(baos));
					break;
				default:
					System.out.println("No data to convert: no compression type");
					return;
				}
				nbtos.writeTag(data);
				rawData = baos.toByteArray();
				length = rawData.length;
				sectors = (byte) (length / 4096 + (length % 4069 == 0 ? 0 : 1));
			} finally {
				if (nbtos != null) {
					nbtos.close();
				}
			}
		}

		//can i actually save the data now please??
		raf.seek(offset);
		raf.writeInt(length);
		raf.write(compressionType.getByte());
		System.out.println("length of raw data: " + rawData.length);
		raf.write(rawData);
		//thank you!!

		this.offset = offset;
	}

	public void loadData(RandomAccessFile raf) throws Exception {
		raf.seek(offset + 5);
		MCAInputStream in = new MCAInputStream(raf);
		NBTInputStream nbtIn = null;
		Tag tag;

		try {
			switch (compressionType) {
			case GZIP:
				nbtIn = new NBTInputStream(new BufferedInputStream(new GZIPInputStream(in)));
				break;
			case ZLIB:
				nbtIn = new NBTInputStream(new BufferedInputStream(new InflaterInputStream(in)));
				break;
			case NONE:
				data = null;
				return;
			}
			tag = nbtIn.readTag();
		} finally {
			if (nbtIn != null) {
				nbtIn.close();
			}
		}

		if (tag instanceof CompoundTag) {
			data = (CompoundTag) tag;
		} else {
			throw new Exception("Invalid chunk data: tag is not of type CompoundTag");
		}
	}

	public void drawImage(ChunkDataProcessor chunkDataProcessor, ColorMapping colorMapping, int x, int z, BufferedImage image) {
		if (data != null) {
			chunkDataProcessor.drawImage2(data, colorMapping, x, z, image);
		}
	}

	public long getOffset() {
		return offset;
	}

	void setOffset(int sectorOffset) {
		this.offset = ((long) sectorOffset) * 4096;
	}

	int getLengthInSectors() {
		return (length + 4) / 4096 + ((length + 4) % 4096 == 0 ? 0 : 1);
	}

	public int getLength() {
		return length;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public byte getSectors() {
		return sectors;
	}

	public CompressionType getCompressionType() {
		return compressionType;
	}

	public CompoundTag getData() {
		return data;
	}
}
