package net.querz.mcaselector.io.mca;

public enum CompressionType {

	NONE(0), // indicates that there is no data present
	GZIP(1),
	ZLIB(2),
	NONE_EXT(-128), // indicates that the chunk data is oversized and saved in a c.x.z.mcc file
	GZIP_EXT(-127),
	ZLIB_EXT(-126);

	private final byte type;

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

	public CompressionType getExternal() {
		switch (this) {
			case NONE:
				return NONE_EXT;
			case GZIP:
				return GZIP_EXT;
			case ZLIB:
				return ZLIB_EXT;
			default:
				// this is already an external type
				return this;
		}
	}
}
