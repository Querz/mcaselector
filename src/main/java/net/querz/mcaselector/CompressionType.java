package net.querz.mcaselector;

public enum CompressionType {
	NONE(0),
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
}
