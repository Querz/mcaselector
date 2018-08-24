package net.querz.mcaselector.io;

import javafx.scene.image.PixelWriter;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntTag;
import net.querz.nbt.Tag;
import java.io.*;
import java.util.List;
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

	//offset in 4KiB chunks
	public MCAChunkData(int offset, int timestamp, byte sectors) {
		this.offset = ((long) offset) * MCAFile.SECTION_SIZE;
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

	public void loadData(RandomAccessFile raf) throws Exception {
		//offset + length of length (4 bytes) + length of compression type (1 byte)
		raf.seek(offset + 5);
		DataInputStream nbtIn = null;

		switch (compressionType) {
		case GZIP:
			nbtIn = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(raf.getFD())), sectors * MCAFile.SECTION_SIZE));
			break;
		case ZLIB:
			nbtIn = new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(raf.getFD())), sectors * MCAFile.SECTION_SIZE));
			break;
		case NONE:
			data = null;
			return;
		}
		Tag tag = Tag.deserialize(nbtIn, 0);

		if (tag instanceof CompoundTag) {
			data = (CompoundTag) tag;
		} else {
			throw new Exception("Invalid chunk data: tag is not of type CompoundTag");
		}
	}

	//saves to offset provided by raf, because it might be different when data changed
	//returns the number of bytes that were written to the file
	public int saveData(RandomAccessFile raf) throws Exception {
		DataOutputStream nbtOut;

		ByteArrayOutputStream baos;

		switch (compressionType) {
		case GZIP:
			nbtOut = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(baos = new ByteArrayOutputStream()), sectors * MCAFile.SECTION_SIZE));
			break;
		case ZLIB:
			nbtOut = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(baos = new ByteArrayOutputStream()), sectors * MCAFile.SECTION_SIZE));
			break;
		default:
			return 0;
		}

		data.serialize(nbtOut, 0);
		nbtOut.close();

		byte[] rawData = baos.toByteArray();

		raf.writeInt(rawData.length);
		raf.writeByte(compressionType.getByte());
		raf.write(rawData);

		return rawData.length + 5;
	}

	public void changeData(List<Field> fields, boolean force) {
		for (Field field : fields) {
			try {
				if (force) {
					field.force(data);
				} else {
					field.change(data);
				}
			} catch (Exception ex) {
				Debug.dump("error trying to update field: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	public void drawImage(int x, int z, PixelWriter writer) {
		if (data != null) {
			int dataVersion = data.getInt("DataVersion");
			try {
				VersionController.getChunkDataProcessor(dataVersion).drawChunk(
						data,
						VersionController.getColorMapping(dataVersion),
						x, z,
						writer
				);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public long getOffset() {
		return offset;
	}

	void setOffset(int sectorOffset) {
		this.offset = ((long) sectorOffset) * MCAFile.SECTION_SIZE;
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
