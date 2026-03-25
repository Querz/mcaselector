package net.querz.mcaselector.io.db;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.validation.ShutdownHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public final class CacheHandler {

	private static final Logger LOGGER = LogManager.getLogger(CacheHandler.class);

	private CacheHandler() {}

	private static final UUID fileTimeUUID = UUID.nameUUIDFromBytes("file_times".getBytes());
	private static final UUID structuresUUID = UUID.nameUUIDFromBytes("structures".getBytes());
	private static final Options options = new Options();
	private static ShutdownHooks.ShutdownJob closeShutdownHook;
	private static String dbPath;

	static {
		options.createIfMissing(true);
		// Tune LevelDB options for better throughput on overlay cache
		options.writeBufferSize(16 * 1024 * 1024); // 16MB
		options.blockSize(4 * 1024);               // 4KB blocks
		options.compressionType(CompressionType.SNAPPY);
	}

	private static DB db;

	public static synchronized void switchTo(String dbPath) throws IOException {
		removeCloseShutdownHook();
		close();
		File cache = new File(dbPath);

		if (!cache.exists() && !cache.mkdirs()) {
			throw new IOException("failed to create directory for cache db");
		}

		try {
			db = Iq80DBFactory.factory.open(cache, options);
		} catch (IOException e) {
			Iq80DBFactory.factory.destroy(cache, new Options());
			db = Iq80DBFactory.factory.open(cache, options);
		}
		CacheHandler.dbPath = dbPath;
		addCloseShutdownHook();
	}

	public static long getFileTime(Point2i region) throws DBException {
		while (!isInitialized()) {
			Thread.onSpinWait();
		}
		byte[] key = key(region.getX(), region.getZ(), fileTimeUUID);
		byte[] value = db.get(key);
		if (value == null) {
			return -1;
		}
		return bytesToLong(value);
	}

	public static void setFileTime(Point2i region, long time) throws DBException {
		while (!isInitialized()) {
			Thread.onSpinWait();
		}
		byte[] key = key(region.getX(), region.getZ(), fileTimeUUID);
		byte[] value = longToBytes(time);
		db.put(key, value);
	}

	public static int[] getData(Overlay parser, Point2i region) throws DBException, IOException {
		UUID id = UUID.nameUUIDFromBytes((parser.name() + parser.getMultiValuesID()).getBytes());
		byte[] key = key(region.getX(), region.getZ(), id);
		byte[] rawValue = db.get(key);
		if (rawValue == null) {
			return null;
		}
		return readCacheValue(rawValue);
	}

	public static void setData(Overlay parser, Point2i region, int[] data) throws DBException, IOException {
		UUID id = UUID.nameUUIDFromBytes((parser.name() + parser.getMultiValuesID()).getBytes());
		byte[] key = key(region.getX(), region.getZ(), id);
		byte[] rawValue = cacheValue(data);
		db.put(key, rawValue);
	}

	public static void setStructureData(Point2i region, Long2ObjectOpenHashMap<String[]> structures) throws DBException, IOException {
		byte[] key = key(region.getX(), region.getZ(), structuresUUID);
		ByteArrayOutputStream bos;
		try (DataOutputStream dos = new DataOutputStream(bos = new ByteArrayOutputStream())) {
			dos.writeInt(structures.size());
			for (Map.Entry<Long, String[]> entry : structures.long2ObjectEntrySet()) {
				dos.writeLong(entry.getKey());
				int len = Math.min(entry.getValue().length, Byte.MAX_VALUE);
				dos.writeByte(len);
				for (int i = 0; i < len; i++) {
					dos.writeUTF(entry.getValue()[i]);
				}
			}
		}
		byte[] rawValue = bos.toByteArray();
		db.put(key, rawValue);
	}

	public static Long2ObjectOpenHashMap<String[]> getStructureData(Point2i region) throws DBException, IOException {
		while (!isInitialized()) {
			Thread.onSpinWait();
		}
		byte[] key = key(region.getX(), region.getZ(), structuresUUID);
		byte[] rawValue = db.get(key);
		if (rawValue == null) {
			return null;
		}
		try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(rawValue))) {
			int size = dis.readInt();
			Long2ObjectOpenHashMap<String[]> map = new Long2ObjectOpenHashMap<>(size);
			for (int i = 0; i < size; i++) {
				long chunk = dis.readLong();
				int len = dis.readUnsignedByte();
				String[] structures = new String[len];
				for (int j = 0; j < len; j++) {
					structures[j] = dis.readUTF();
				}
				map.put(chunk, structures);
			}
			return map;
		}
	}

	public static void deleteData(Overlay parser, Point2i region) throws DBException {
		UUID id = UUID.nameUUIDFromBytes((parser.name() + parser.getMultiValuesID()).getBytes());
		byte[] key = key(region.getX(), region.getZ(), id);
		db.delete(key);
	}

	public static void deleteData(Point2i region) throws IOException {
		if (!isInitialized()) {
			LOGGER.warn("failed to delete region {} from cache because it hasn't been initialized yet", region);
			return;
		}

		List<byte[]> deleteKeys = new ArrayList<>();

		// create a start- and an end-key to iterate between them to find all values for this region
		byte[] startKey = key(region.getX(), region.getZ(), null);
		byte[] endKey = endKey(region.getX(), region.getZ());
		try (DBIterator iterator = db.iterator()) {
			for (iterator.seek(startKey); iterator.hasNext() && Arrays.compareUnsigned(iterator.peekNext().getKey(), endKey) <= 0; iterator.next()) {
				byte[] key = iterator.peekNext().getKey();
				deleteKeys.add(key);
			}
		}

		try (WriteBatch batch = db.createWriteBatch()) {
			for (byte[] key : deleteKeys) {
				batch.delete(key);
			}
			db.write(batch);
		}
	}

	public static synchronized void clear(File cacheDB, boolean init) throws IOException {
		if (!isInitialized()) {
			if (cacheDB != null) {
				Iq80DBFactory.factory.destroy(cacheDB, new Options());
			}
			return;
		}
		File dbFile = new File(dbPath);
		close();
		Iq80DBFactory.factory.destroy(dbFile, new Options());
		LOGGER.debug("deleted cache db {}", dbFile.getCanonicalPath());
		if (init) {
			switchTo(dbFile.getPath());
		}
	}

	public static boolean isInitialized() {
		return db != null && dbPath != null;
	}

	private static void addCloseShutdownHook() {
		if (closeShutdownHook == null) {
			closeShutdownHook = ShutdownHooks.addShutdownHook(CacheHandler::closeOnShutdown, 100);
		} else {
			throw new RuntimeException("attempted to add a shutdown hook for cache db while one was already present");
		}
	}

	private static void removeCloseShutdownHook() {
		if (closeShutdownHook != null) {
			ShutdownHooks.removeShutdownHook(closeShutdownHook);
			closeShutdownHook = null;
		}
	}

	private static void closeOnShutdown() {
		try {
			close();
		} catch (IOException ex) {
			LOGGER.warn("failed to close cache db with exception", ex);
		}
	}

	private static void close() throws IOException {
		if (db != null) {
			db.close();
			dbPath = null;
			db = null;
		}
	}

	private static byte[] key(int xPos, int zPos, UUID uuid) {
		ByteBuffer buf = ByteBuffer.allocate(24);
		buf.putInt(xPos);
		buf.putInt(zPos);
		if (uuid != null) {
			buf.putLong(uuid.getMostSignificantBits());
			buf.putLong(uuid.getLeastSignificantBits());
		}
		return buf.array();
	}

	private static byte[] endKey(int xPos, int zPos) {
		ByteBuffer buf = ByteBuffer.allocate(24);
		buf.putInt(xPos);
		buf.putInt(zPos);
		buf.putLong(0xFFFFFFFFFFFFFFFFL);
		buf.putLong(0xFFFFFFFFFFFFFFFFL);
		return buf.array();
	}

	private static byte[] cacheValue(int[] data) {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		IntBuffer iBuf = buf.asIntBuffer();
		iBuf.put(data);
		return buf.array();
	}

	private static int[] readCacheValue(byte[] value) {
		int[] data = new int[1024];
		ByteBuffer.wrap(value).asIntBuffer().get(data);
		return data;
	}

	private static byte[] longToBytes(long l) {
		byte[] result = new byte[8];
		for (int i = 7; i >= 0; i--) {
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	private static long bytesToLong(final byte[] b) {
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result <<= 8;
			result |= (b[i] & 0xFF);
		}
		return result;
	}
}
