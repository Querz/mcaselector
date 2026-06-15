package net.querz.mcaselector.version.mapping.util;

import net.querz.mcaselector.util.exception.ThrowingBiConsumer;
import java.io.*;
import java.nio.file.Path;

public final class Command {

	private Command() {}

	public static void exec(Path workingDirectory, String... cmd) throws InterruptedException, IOException {
		exec(workingDirectory, null, cmd);
	}

	public static void exec(Path workingDirectory, ThrowingBiConsumer<String, Process, IOException> outputConsumer, String... cmd) throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectErrorStream(true);
		System.out.println(pb.command());
		pb.directory(workingDirectory.toFile());
		Process pr = pb.start();
		try (BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
			String line;
			while ((line = buf.readLine()) != null) {
				System.out.println(line);
				if (outputConsumer != null) {
					outputConsumer.accept(line, pr);
				}
			}
		}
		pr.waitFor();
	}

	public static void sendToProcess(Process process, String line) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
			writer.write(line);
		}
	}
}
