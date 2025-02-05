package net.querz.mcaselector.version.mapping.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

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

	public static void redirect(Path workingDirectory, String... cmd) throws InterruptedException, IOException {
		String[] a = null, b = null;
		for (int i = 0; i < cmd.length; i++) {
			if (cmd[i].equals("|")) {
				a = new String[i];
				System.arraycopy(cmd, 0, a, 0, i);
				b = new String[cmd.length - i - 1];
				System.arraycopy(cmd, i + 1, b, 0, b.length);
				break;
			}
		}
		if (a == null) {
			throw new IllegalArgumentException("expected pipe");
		}

		List<Process> processes = ProcessBuilder.startPipeline(List.of(
				new ProcessBuilder(a).directory(workingDirectory.toFile()).inheritIO().redirectOutput(ProcessBuilder.Redirect.PIPE),
				new ProcessBuilder(b).directory(workingDirectory.toFile()).redirectError(ProcessBuilder.Redirect.INHERIT)
		));

		Process pr = processes.getLast();
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
