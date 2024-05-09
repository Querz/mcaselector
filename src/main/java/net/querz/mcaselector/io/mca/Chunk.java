package net.querz.mcaselector.io.mca;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.querz.io.ExposedByteArrayOutputStream;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;
import net.querz.nbt.io.NBTReader;
import net.querz.nbt.io.NBTWriter;
import net.querz.nbt.CompoundTag;
import java.io.*;
import java.util.List;
import java.util.function.Function;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public abstract class Chunk {

	protected int timestamp;
	protected CompoundTag data;
	protected CompressionType compressionType;
	protected final Point2i absoluteLocation;

	public Chunk(Point2i absoluteLocation) {
		this.absoluteLocation = absoluteLocation;
	}

	public void load(ByteArrayPointer ptr) throws IOException {
		int length = ptr.readInt();
		compressionType = CompressionType.fromByte(ptr.readByte());

		DataInputStream nbtIn = switch (compressionType) {
			case GZIP -> new DataInputStream(new BufferedInputStream(new GZIPInputStream(ptr, length)));
			case ZLIB -> new DataInputStream(new BufferedInputStream(new InflaterInputStream(ptr, new Inflater(), length)));
			case LZ4 -> new DataInputStream(new BufferedInputStream(new LZ4BlockInputStream(ptr)));
			case NONE, UNCOMPRESSED -> new DataInputStream(ptr);
			case GZIP_EXT -> new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(getMCCFile()))));
			case ZLIB_EXT -> new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(getMCCFile()))));
			case LZ4_EXT -> new DataInputStream(new BufferedInputStream(new LZ4BlockInputStream(new FileInputStream(getMCCFile()))));
			case NONE_EXT, UNCOMPRESSED_EXT -> new DataInputStream(new BufferedInputStream(new FileInputStream(getMCCFile())));
		};

		Tag tag = new NBTReader().read(nbtIn);

		if (tag instanceof CompoundTag) {
			data = (CompoundTag) tag;
		} else {
			throw new IOException("unexpected chunk data tag type " + tag.getType() + ", expected " + Tag.Type.COMPOUND);
		}
	}

	public void load(RandomAccessFile raf) throws IOException {
		int length = raf.readInt();
		compressionType = CompressionType.fromByte(raf.readByte());

		DataInputStream nbtIn = switch (compressionType) {
			case GZIP -> new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(raf.getFD()))));
			case ZLIB -> new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(raf.getFD()))));
			case LZ4 -> new DataInputStream(new BufferedInputStream(new LZ4BlockInputStream(new FileInputStream(raf.getFD()))));
			case NONE, UNCOMPRESSED -> new DataInputStream(new BufferedInputStream(new FileInputStream(raf.getFD()), length - 1));
			case GZIP_EXT -> new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(getMCCFile()))));
			case ZLIB_EXT -> new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(getMCCFile()))));
			case LZ4_EXT -> new DataInputStream(new BufferedInputStream(new LZ4BlockInputStream(new FileInputStream(getMCCFile()))));
			case NONE_EXT, UNCOMPRESSED_EXT -> new DataInputStream(new BufferedInputStream(new FileInputStream(getMCCFile())));
		};

		Tag tag = new NBTReader().read(nbtIn);

		if (tag instanceof CompoundTag) {
			data = (CompoundTag) tag;
		} else {
			throw new IOException("unexpected chunk data tag type " + tag.getType() + ", expected " + Tag.Type.COMPOUND);
		}
	}

	public int save(RandomAccessFile raf) throws IOException {
		ExposedByteArrayOutputStream baos = null;

		DataOutputStream nbtOut = switch (compressionType) {
			case GZIP, GZIP_EXT -> new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(baos = new ExposedByteArrayOutputStream())));
			case ZLIB, ZLIB_EXT -> new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(baos = new ExposedByteArrayOutputStream())));
			case LZ4, LZ4_EXT -> new DataOutputStream(new BufferedOutputStream(new LZ4BlockOutputStream(baos = new ExposedByteArrayOutputStream())));
			case NONE, NONE_EXT, UNCOMPRESSED, UNCOMPRESSED_EXT -> new DataOutputStream(new BufferedOutputStream(baos = new ExposedByteArrayOutputStream()));
		};

		new NBTWriter().write(nbtOut, data);
		nbtOut.close();

		// save mcc file if chunk doesn't fit in mca file
		if (baos.size() > 1048576) {
			// if the chunk's version is below 2203, we throw an exception instead
			int dataVersion = data.getInt("DataVersion");
			if (dataVersion < 2203) {
				throw new RuntimeException("chunk at " + absoluteLocation + " is oversized and can't be saved when DataVersion is below 2203");
			}

			raf.writeInt(1);
			raf.writeByte(compressionType.getExternal().getByte());
			try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(getMCCFile()), baos.size())) {
				bos.write(baos.getBuffer(), 0, baos.size());
			}
			return 5;
		} else {
			raf.writeInt(baos.size() + 1); // length includes the compression type byte
			raf.writeByte(compressionType.getByte());
			raf.write(baos.getBuffer(), 0, baos.size());
			return baos.size() + 5; // data length + 1 compression type byte + 4 length bytes
		}
	}

	public abstract boolean relocate(Point3i offset);

	public abstract void merge(CompoundTag destination, List<Range> ranges, int yOffset);

	public abstract File getMCCFile();

	public boolean isEmpty() {
		return data == null;
	}

	public CompoundTag getData() {
		return data;
	}

	public void setData(CompoundTag data) {
		this.data = data;
	}

	public CompressionType getCompressionType() {
		return compressionType;
	}

	public void setCompressionType(CompressionType compressionType) {
		this.compressionType = compressionType;
	}

	public Point2i getAbsoluteLocation() {
		return absoluteLocation;
	}

	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		String s = NBTUtil.toSNBT(data);
		return "<absoluteLocation=" + absoluteLocation + ", compressionType=" + compressionType + ", data=" + s + ">";
	}

	protected <T extends Chunk> T clone(Function<Point2i, T> chunkConstructor) {
		T clone = chunkConstructor.apply(absoluteLocation);
		clone.compressionType = compressionType;
		clone.timestamp = timestamp;
		if (data != null) {
			clone.data = data.copy();
		}
		return clone;
	}
}
