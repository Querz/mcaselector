package net.querz.mcaselector.version.mapping.minecraft;

import net.querz.mcaselector.version.mapping.util.Command;
import java.io.IOException;
import java.nio.file.Path;

public final class Report {

	private Report() {}

	public static void generate(Path serverJar, Path output) throws IOException, InterruptedException {
		Command.exec(serverJar.getParent(),
				"java", "-DbundlerMainClass=net.minecraft.data.Main",
				"-jar", serverJar.toAbsolutePath().toString(),
				"--reports", "--server",
				"--output", output.toAbsolutePath().toString()
		);
	}
}
