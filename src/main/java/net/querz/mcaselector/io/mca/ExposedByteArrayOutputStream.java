package net.querz.mcaselector.io.mca;

import java.io.ByteArrayOutputStream;

/**
 * ExposedByteArrayOutputStream exposes the buffer array in #getBuffer()
 * to avoid calling Arrays.copyOf() when using ByteArrayOutputStream#toByteArray()
 */
class ExposedByteArrayOutputStream extends ByteArrayOutputStream {

	public ExposedByteArrayOutputStream() {
		super();
	}

	public ExposedByteArrayOutputStream(int size) {
		super(size);
	}

	public byte[] getBuffer() {
		return buf;
	}
}
