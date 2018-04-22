package net.querz.mcaselector;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class MCAInputStream extends InputStream {

	private RandomAccessFile raf;

	public MCAInputStream(RandomAccessFile raf) {
		this.raf = raf;
	}

	@Override
	public int read() throws IOException {
		return raf.read();
	}
}
