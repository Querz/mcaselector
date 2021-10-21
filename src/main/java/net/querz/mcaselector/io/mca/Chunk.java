package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.io.NBTDeserializer;
import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.io.SNBTUtil;
import net.querz.nbt.tag.CompoundTag;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
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
			case NONE -> new DataInputStream(ptr);
			case GZIP_EXT -> new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(getMCCFile()))));
			case ZLIB_EXT -> new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(getMCCFile()))));
			case NONE_EXT -> new DataInputStream(new BufferedInputStream(new FileInputStream(getMCCFile())));
		};

		NamedTag tag = new NBTDeserializer(false).fromStream(nbtIn);

		if (tag.getTag() instanceof CompoundTag) {
			data = (CompoundTag) tag.getTag();
		} else {
			throw new IOException("unexpected chunk data tag type " + tag.getTag().getID() + ", expected " + CompoundTag.ID);
		}
	}

	public void load(RandomAccessFile raf) throws IOException {
		int length = raf.readInt();
		compressionType = CompressionType.fromByte(raf.readByte());

		DataInputStream nbtIn = switch (compressionType) {
			case GZIP -> new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(raf.getFD()))));
			case ZLIB -> new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(raf.getFD()))));
			case NONE -> new DataInputStream(new BufferedInputStream(new FileInputStream(raf.getFD()), length - 1));
			case GZIP_EXT -> new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(getMCCFile()))));
			case ZLIB_EXT -> new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FileInputStream(getMCCFile()))));
			case NONE_EXT -> new DataInputStream(new BufferedInputStream(new FileInputStream(getMCCFile())));
		};

		NamedTag tag = new NBTDeserializer(false).fromStream(nbtIn);

		if (tag.getTag() instanceof CompoundTag) {
			data = (CompoundTag) tag.getTag();
		} else {
			throw new IOException("unexpected chunk data tag type " + tag.getTag().getID() + ", expected " + CompoundTag.ID);
		}
	}

	public int save(RandomAccessFile raf) throws IOException {
		ExposedByteArrayOutputStream baos = null;

		DataOutputStream nbtOut = switch (compressionType) {
			case GZIP, GZIP_EXT -> new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(baos = new ExposedByteArrayOutputStream())));
			case ZLIB, ZLIB_EXT -> new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(baos = new ExposedByteArrayOutputStream())));
			case NONE, NONE_EXT -> new DataOutputStream(new BufferedOutputStream(baos = new ExposedByteArrayOutputStream()));
		};

		new NBTSerializer(false).toStream(new NamedTag(null, data), nbtOut);
		nbtOut.close();

		// save mcc file if chunk doesn't fit in mca file
		if (baos.size() > 1048576) {
			// if the chunk's version is below 2203, we throw an exception instead
			Integer dataVersion = ValidationHelper.withDefault(() -> data.getInt("DataVersion"), null);
			if (dataVersion == null) {
				throw new RuntimeException("no DataVersion for oversized chunk");
			}
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
		String s;
		try {
			 s = SNBTUtil.toSNBT(data);
		} catch (IOException e) {
			s = "error";
		}
		return "<absoluteLoaction=" + absoluteLocation + ", compressionType=" + compressionType + ", data=" + s + ">";
	}
}
