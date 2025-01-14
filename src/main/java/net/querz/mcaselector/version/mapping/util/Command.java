package net.querz.mcaselector.version.mapping.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;

public final class Command {

	private Command() {}

	public static void exec(Path workingDirectory, String... cmd) throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		System.out.println(pb.command());
		pb.directory(workingDirectory.toFile());
		Process pr = pb.start();
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line;
		while ((line = buf.readLine()) != null) {
			System.out.println(line);
		}
		BufferedReader errBuf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		while ((line = errBuf.readLine()) != null) {
			System.out.println(line);
		}
		pr.waitFor();
	}
}
