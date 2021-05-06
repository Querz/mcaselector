package net.querz.mcaselector.io;

import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import java.io.InputStream;

public class ByteArrayReader extends InputStream {

	private final byte[] data;
	private int pointer = 0;

	public ByteArrayReader(byte[] data) {
		this.data = data;
	}

	public void seek(long pointer) {
		this.pointer = (int) pointer;
	}

	public byte readByte() throws ArrayIndexOutOfBoundsException {
		return data[pointer++];
	}

	public int readInt() throws ArrayIndexOutOfBoundsException {
		int i = (data[pointer++] & 0xFF) << 24;
		i |= (data[pointer++] & 0xFF) << 16;
		i |= (data[pointer++] & 0xFF) << 8;
		return i | data[pointer++] & 0xFF;
	}

	public float readFloat() throws ArrayIndexOutOfBoundsException {
		return Float.intBitsToFloat(readInt());
	}

	public double readDouble() throws ArrayIndexOutOfBoundsException {
		return Double.longBitsToDouble(readLong());
	}

	public long readLong() throws ArrayIndexOutOfBoundsException {
		return ((long) readInt()) << 32 | readInt();
	}

	public Point2i readPoint2i() throws ArrayIndexOutOfBoundsException {
		return new Point2i(readInt(), readInt());
	}

	public Point2f readPoint2f() throws ArrayIndexOutOfBoundsException {
		return new Point2f(Float.intBitsToFloat(readInt()), Float.intBitsToFloat(readInt()));
	}

	public Point3i readPoint3i() throws ArrayIndexOutOfBoundsException {
		return new Point3i(readInt(), readInt(), readInt());
	}

	public boolean hasNext() {
		return pointer < data.length;
	}

	@Override
	public int read() throws ArrayIndexOutOfBoundsException {
		return data[pointer++] & 0xFF;
	}
}
