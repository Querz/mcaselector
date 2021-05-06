package net.querz.mcaselector.io;

import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import java.io.OutputStream;

public class ByteArrayWriter extends OutputStream {

	private final byte[] data;
	private int pointer = 0;

	public ByteArrayWriter(byte[] data) {
		this.data = data;
	}

	public void seek(long pointer) {
		this.pointer = (int) pointer;
	}

	public void writeByte(byte b) throws ArrayIndexOutOfBoundsException {
		data[pointer++] = b;
	}

	public void writeInt(int i) throws ArrayIndexOutOfBoundsException {
		data[pointer++] = (byte) (i >> 24);
		data[pointer++] = (byte) ((i >> 16) & 0xFF);
		data[pointer++] = (byte) ((i >> 8) & 0xFF);
		data[pointer++] = (byte) i;
	}

	public void writeFloat(float f) throws ArrayIndexOutOfBoundsException {
		writeInt(Float.floatToIntBits(f));
	}

	public void writeDouble(double d) throws ArrayIndexOutOfBoundsException {
		writeLong(Double.doubleToLongBits(d));
	}

	public void writeLong(long l) throws ArrayIndexOutOfBoundsException {
		writeInt((int) (l >> 32));
		writeInt((int) l);
	}

	public void writePoint2i(Point2i p) throws ArrayIndexOutOfBoundsException {
		writeInt(p.getX());
		writeInt(p.getZ());
	}

	public void writePoint2f(Point2f p) throws ArrayIndexOutOfBoundsException {
		writeFloat(p.getX());
		writeFloat(p.getY());
	}

	public void writePoint3i(Point3i p) throws ArrayIndexOutOfBoundsException {
		writeInt(p.getX());
		writeInt(p.getY());
		writeInt(p.getZ());
	}

	@Override
	public void write(int b) throws ArrayIndexOutOfBoundsException {
		data[pointer++] = (byte) b;
	}
}
