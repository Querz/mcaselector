package net.querz.mcaselector;

import junit.framework.TestCase;
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
		System.out.printf("took %s to read mca file %s\n", t, mcaFile.getFile().getName());
		return mcaFile;
	}

	public static String calculateFileMD5(File file) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException ex) {
			TestCase.fail(ex.getMessage());
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
			TestCase.fail(ex.getMessage());
		}
		return byteArrayToHexString(md.digest());
	}

	public static String byteArrayToHexString(byte[] bytes) {
		BigInteger bi = new BigInteger(1, bytes);
		return String.format("%0" + (bytes.length << 1) + "X", bi);
	}
}
