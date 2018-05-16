package net.querz.mcaselector.io;

public enum CompressionType {

	NONE(0), // indicates that there is no data present
	GZIP(1),
	ZLIB(2);

	private byte type;

	CompressionType(int type) {
		this.type = (byte) type;
	}

	public static CompressionType fromByte(byte t) {
		for (CompressionType c : CompressionType.values()) {
			if (c.type == t) {
				return c;
			}
		}
		throw new IllegalArgumentException("Invalid compression type " + t);
	}

	public byte getByte() {
		return type;
	}
}
