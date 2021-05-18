package net.querz.mcaselector;

import junit.framework.TestCase;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.progress.Timer;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;
import static org.junit.Assert.*;

public final class MCASelectorTestCase {

	public static File getResourceFile(String resource) {
		URL resFileURL = MCASelectorTestCase.class.getClassLoader().getResource(resource);
		assertNotNull(resFileURL);
		return new File(resFileURL.getFile());
	}

	public static byte[] loadDataFromResource(String resource) throws IOException {
		return Files.readAllBytes(getResourceFile(resource).toPath());
	}

	public static RegionMCAFile loadRegionMCAFileFromResource(String resource) throws IOException {
		Timer t = new Timer();
		ByteArrayPointer ptr = new ByteArrayPointer(loadDataFromResource(resource));
		RegionMCAFile mcaFile = new RegionMCAFile(new File(resource));
		mcaFile.load(ptr);
		Debug.dumpf("took %s to read mca file %s", t, mcaFile.getFile().getName());
		return mcaFile;
	}

	public static String calculateFileMD5(File file) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException ex) {
			fail(ex.getMessage());
		}
		try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
			byte[] buffer = new byte[8192];
			int numRead;
			do {
				numRead = bis.read(buffer);
				if (numRead > 0) {
					md.update(buffer, 0, numRead);
				}
			} while (numRead != -1);
		} catch (IOException ex) {
			fail(ex.getMessage());
		}
		return byteArrayToHexString(md.digest());
	}

	public static String byteArrayToHexString(byte[] bytes) {
		BigInteger bi = new BigInteger(1, bytes);
		return String.format("%0" + (bytes.length << 1) + "X", bi);
	}

	public static <E extends Exception> void assertThrowsException(ExceptionRunnable<E> r, Class<? extends Exception> e) {
		assertThrowsException(r, e, false);
	}

	public static <E extends Exception> void assertThrowsException(ExceptionRunnable<E> r, Class<? extends Exception> e, boolean printStackTrace) {
		try {
			r.run();
			fail();
		} catch (Exception ex) {
			if (printStackTrace) {
				ex.printStackTrace();
			}
			TestCase.assertEquals(ex.getClass(), e);
		}
	}

	public static <E extends Exception> void assertThrowsNoException(ExceptionRunnable<E> r) {
		try {
			r.run();
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Threw exception " + ex.getClass().getName() + " with message \"" + ex.getMessage() + "\"");
		}
	}

	public static <T, E extends Exception> void assertThrowsException(ExceptionSupplier<T, E> r, Class<? extends Exception> e) {
		assertThrowsException(r, e, false);
	}

	public static <T, E extends Exception> void assertThrowsException(ExceptionSupplier<T, E> r, Class<? extends Exception> e, boolean printStackTrace) {
		try {
			r.run();
			fail();
		} catch (Exception ex) {
			if (printStackTrace) {
				ex.printStackTrace();
			}
			TestCase.assertEquals(ex.getClass(), e);
		}
	}

	public static <T, E extends Exception> T assertThrowsNoException(ExceptionSupplier<T, E> r) {
		return assertThrowsNoException(r, false);
	}

	public static <T, E extends Exception> T assertThrowsNoException(ExceptionSupplier<T, E> r, boolean printStackTrace) {
		try {
			return r.run();
		} catch (Exception ex) {
			if (printStackTrace) {
				ex.printStackTrace();
			}
			fail("Threw exception " + ex.getClass().getName() + " with message \"" + ex.getMessage() + "\"");
		}
		return null;
	}

	public static void assertThrowsRuntimeException(Runnable r, Class<? extends Exception> e) {
		assertThrowsRuntimeException(r, e, false);
	}

	public static void assertThrowsRuntimeException(Runnable r, Class<? extends Exception> e, boolean printStackTrace) {
		try {
			r.run();
			fail();
		} catch (Exception ex) {
			if (printStackTrace) {
				ex.printStackTrace();
			}
			TestCase.assertEquals(e, ex.getClass());
		}
	}

	public static void assertThrowsRuntimeException(Runnable r, boolean printStackTrace) {
		try {
			r.run();
			fail();
		} catch (Exception ex) {
			if (printStackTrace) {
				ex.printStackTrace();
			}
		}
	}

	public static void assertThrowsNoRuntimeException(Runnable r) {
		assertThrowsNoRuntimeException(() -> {
			r.run();
			return null;
		});
	}

	public static <T> T assertThrowsNoRuntimeException(Supplier<T> r) {
		try {
			return r.get();
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Threw exception " + ex.getClass().getName() + " with message \"" + ex.getMessage() + "\"");
		}
		return null;
	}
}
