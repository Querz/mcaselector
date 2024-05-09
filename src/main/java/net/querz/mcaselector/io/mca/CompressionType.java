package net.querz.mcaselector.io.mca;

public enum CompressionType {

	NONE(0), // indicates that there is no data present
	GZIP(1),
	ZLIB(2),
	UNCOMPRESSED(3),
	LZ4(4),
	NONE_EXT(-128), // indicates that the chunk data is oversized and saved in a c.x.z.mcc file
	GZIP_EXT(-127),
	ZLIB_EXT(-126),
	UNCOMPRESSED_EXT(-125),
	LZ4_EXT(-124);

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
		return switch (this) {
			case NONE -> NONE_EXT;
			case GZIP -> GZIP_EXT;
			case ZLIB -> ZLIB_EXT;
			case UNCOMPRESSED -> UNCOMPRESSED_EXT;
			case LZ4 -> LZ4_EXT;
			default ->
				// this is already an external type
				this;
		};
	}
}
