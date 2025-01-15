package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersionFile;
import net.querz.mcaselector.version.mapping.minecraft.Report;
import net.querz.mcaselector.version.mapping.util.Download;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class StructureConfig {

	@SerializedName("structures") private Set<String> structures;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	public StructureConfig() {
		structures = new HashSet<>();
	}

	public static ColorConfig load(Path path) throws IOException {
		return GSON.fromJson(Files.newBufferedReader(path), ColorConfig.class);
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(this);
		Files.writeString(path, json);
	}

	public void generate(MinecraftVersion version, Path tmp) throws IOException, InterruptedException {
		Path versionJson = tmp.resolve("version.json");
		Path serverJar = tmp.resolve("server.jar");
		Path generated = tmp.resolve("generated");

		MinecraftVersionFile versionFile;
		// download version.json
		if (Files.exists(versionJson)) {
			versionFile = MinecraftVersionFile.of(versionJson);
		} else {
			versionFile = MinecraftVersionFile.download(version, versionJson);
		}

		// download server jar
		if (!Files.exists(serverJar)) {
			Download.to(versionFile.getDownloads().server().url(), serverJar);
		}

		// generate reports
		if (!Files.exists(generated)) {
			Report.generate(serverJar, generated);
		}

		Path structure = generated.resolve("data/minecraft/worldgen/structure");
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(structure)) {
			for (Path s : ds) {
				if (!Files.isRegularFile(s)) {
					continue;
				}
				String fileName = s.getFileName().toString();
				String name = fileName.substring(0, fileName.length() - 5);
				structures.add(name);
			}
		}
	}
}
