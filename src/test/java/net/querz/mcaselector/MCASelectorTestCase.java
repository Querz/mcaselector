package net.querz.mcaselector;

import junit.framework.TestCase;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

public class MCASelectorTestCase extends TestCase {

	protected File getResourceFile(String resource) {
		URL resFileURL = getClass().getClassLoader().getResource(resource);
		assertNotNull(resFileURL);
		return new File(resFileURL.getFile());
	}

	protected byte[] loadDataFromResource(String resource) throws IOException {
		return Files.readAllBytes(getResourceFile(resource).toPath());
	}
}
