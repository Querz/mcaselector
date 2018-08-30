package net.querz.mcaselector.io;

import java.io.IOException;
import java.io.InputStream;

public class ByteArrayPointer extends InputStream {

	private byte[] data;
	private int pointer = 0;

	public ByteArrayPointer(byte[] data) {
		this.data = data;
	}

	public void seek(long pointer) {
		this.pointer = (int) pointer;
	}

	public byte readByte() {
		return data[pointer++];
	}

	public int readInt() {
		int i = (data[pointer++] & 0xFF) << 24;
		i |= (data[pointer++] & 0xFF) << 16;
		i |= (data[pointer++] & 0xFF) << 8;
		return i | data[pointer++] & 0xFF;
	}

	@Override
	public int read() throws IOException {
		try {
			return data[pointer++] & 0xFF;
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new IOException(ex);
		}
	}
}
