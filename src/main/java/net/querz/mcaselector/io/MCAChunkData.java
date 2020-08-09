package net.querz.mcaselector.io;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public class MCAChunkData {

	private final long offset; //in actual bytes
	private final int timestamp;
	private final byte sectors;
	private int length; //length without padding
	private CompressionType compressionType;
	private CompoundTag data;

	private final Point2i absoluteLocation;

	private static final Random random = new Random();

	@Override
	public String toString() {
		return  "offset=" + offset +
				"\ntimestamp=" + timestamp +
				"\nsectors=" + sectors +
				"\nlength=" + length +
				"\ncompressionType=" + compressionType +
				"\ndata=" + data +
				"\nabsoluteLocation=" + absoluteLocation;
	}

	//offset in 4KiB chunks
	public MCAChunkData(Point2i absoluteLocation, int offset, int timestamp, byte sectors) {
		this.absoluteLocation = absoluteLocation;
		this.offset = ((long) offset) * MCAFile.SECTION_SIZE;
		this.timestamp = timestamp;
		this.sectors = sectors;
	}

	static MCAChunkData newEmptyLevelMCAChunkData(Point2i absoluteLocation, int dataVersion) {
		MCAChunkData mcaChunkData = new MCAChunkData(absoluteLocation, 0, 0, (byte) 1);
		CompoundTag root = new CompoundTag();
		CompoundTag level = new CompoundTag();
		level.putInt("xPos", absoluteLocation.getX());
		level.putInt("zPos", absoluteLocation.getZ());
		level.putString("Status", "full");
		root.put("Level", level);
		root.putInt("DataVersion", dataVersion);
		mcaChunkData.data = root;
		mcaChunkData.compressionType = CompressionType.ZLIB;
		return mcaChunkData;
	}

	public boolean isEmpty() {
		return offset == 0 && timestamp == 0 && sectors == 0 || data == null;
	}

	public void readHeader(ByteArrayPointer ptr) {
		ptr.seek(offset);
		length = ptr.readInt();
		compressionType = CompressionType.fromByte(ptr.readByte());
	}

	public void readHeader(RandomAccessFile raf) throws IOException {
		raf.seek(offset);
		length = raf.readInt();
		compressionType = CompressionType.fromByte(raf.readByte());
	}

	public void loadData(ByteArrayPointer ptr) throws Exception {
		//offset + length of length (4 bytes) + length of compression type (1 byte)
		ptr.seek(offset + 5);
		DataInputStream nbtIn = null;

		switch (compressionType) {
		case GZIP:
			nbtIn = new DataInputStream(new BufferedInputStream(new GZIPInputStream(ptr)));
			break;
		case ZLIB:
			nbtIn = new DataInputStream(new BufferedInputStream(new InflaterInputStream(ptr)));
			break;
		case NONE:
			data = null;
			return;
		}
		NamedTag tag = new NBTDeserializer(false).fromStream(nbtIn);

		if (tag.getTag() instanceof CompoundTag) {
			data = (CompoundTag) tag.getTag();
		} else {
			throw new Exception("Invalid chunk data: tag is not of type CompoundTag");
		}
	}

	public void loadData(RandomAccessFile raf) throws IOException {
		raf.seek(offset + 5);
		DataInputStream nbtIn = null;

		switch (compressionType) {
			case GZIP:
				nbtIn = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(raf.getFD()))));
				break;
			case ZLIB:
				nbtIn = new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(raf.getFD()))));
				break;
			case NONE:
				data = null;
				return;
		}

		NamedTag tag = new NBTDeserializer(false).fromStream(nbtIn);

		if (tag.getTag() instanceof CompoundTag) {
			data = (CompoundTag) tag.getTag();
		} else {
			throw new IOException("Invalid chunk data: tag is not of type CompoundTag");
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

		new NBTSerializer(false).toStream(new NamedTag(null, data), nbtOut);

		nbtOut.close();

		byte[] rawData = baos.toByteArray();

		raf.writeInt(rawData.length + 1); // length includes the compression type byte
		raf.writeByte(compressionType.getByte());
		raf.write(rawData);

		return rawData.length + 5;
	}

	public void changeData(List<Field<?>> fields, boolean force) {
		for (Field<?> field : fields) {
			try {
				if (force) {
					field.force(data);
				} else {
					field.change(data);
				}
			} catch (Exception ex) {
				Debug.dumpf("failed to change field %s in chunk %s: %s", field.getType(), absoluteLocation, ex);
			}
		}
	}

	public long getOffset() {
		return offset;
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

	public void setData(CompoundTag data) {
		this.data = data;
	}

	public void setCompressionType(CompressionType compressionType) {
		this.compressionType = compressionType;
	}

	public Point2i getAbsoluteLocation() {
		return absoluteLocation;
	}

	// offset is in blocks
	public boolean relocate(Point2i offset) {
		ChunkRelocator r = VersionController.getChunkRelocator(data.getInt("DataVersion"));
		return r.relocateChunk(data, offset);
	}
}
