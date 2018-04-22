package net.querz.mcaselector;

import java.io.*;

public class MCAFile {
	private File file;
	private int[] offsets;
	private byte[] sectors;
	private int[] timestamps;

	public MCAFile(File file) {
		this.file = file;
		offsets = new int[1024];
		sectors = new byte[1024];
		timestamps = new int[1024];
	}

	public void read(DataInputStream dis) throws IOException {
		for (int i = 0; i < offsets.length; i++) {
			int offset = dis.readByte() & 0xFF;
			offset <<= 8;
			offset |= dis.readByte() & 0xFF;
			offset <<= 8;
			offset |= dis.readByte() & 0xFF;
			offsets[i] = offset;
			sectors[i] = dis.readByte();
		}
		for (int i = 0; i < timestamps.length; i++) {
			timestamps[i] = dis.readInt();
		}
	}

	public MCAChunkData getChunkData(int index) {
		return new MCAChunkData(offsets[index], timestamps[index], sectors[index]);
	}

	public File getFile() {
		return file;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < offsets.length; i++) {
			sb.append(offsets[i]).append(" ").append(sectors[i]).append("; ");
		}
		sb.append("\n");
		for (int timestamp : timestamps) {
			sb.append(timestamp).append("; ");
		}
		return sb.toString();
	}
}
