package net.querz.mcaselector.version.mapping.util;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class Download {

	public static void to(String url, Path output) throws IOException {
		Files.createDirectories(output.getParent());
		try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URI(url).toURL().openStream());
			 FileChannel fileChannel = FileChannel.open(output, WRITE, CREATE, TRUNCATE_EXISTING)) {
			fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		} catch (Exception ex) {
			throw new IOException("failed to download file from " + url + " to " + output, ex);
		}
	}
}
