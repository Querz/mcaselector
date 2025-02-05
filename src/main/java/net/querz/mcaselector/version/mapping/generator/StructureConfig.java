package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersionFile;
import net.querz.mcaselector.version.mapping.minecraft.Report;
import net.querz.mcaselector.version.mapping.util.CollectionAdapter;
import net.querz.mcaselector.version.mapping.util.Download;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class StructureConfig {

	private final Set<String> structures;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeHierarchyAdapter(Set.class, new CollectionAdapter())
			.create();

	public StructureConfig() {
		structures = new HashSet<>();
	}

	public StructureConfig(Set<String> structures) {
		this.structures = structures;
	}

	public static StructureConfig load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return load(reader);
		}
	}

	public static StructureConfig load(Reader reader) throws IOException {
		return new StructureConfig(GSON.fromJson(reader, new TypeToken<>() {}));
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(structures);
		Files.writeString(path, json);
	}

	public void merge(StructureConfig other) {
		structures.addAll(other.structures);
	}

	public void generate(MinecraftVersion version, Path tmp) throws IOException, InterruptedException {
		Path versionJson = tmp.resolve("version.json");
		Path serverJar = tmp.resolve("server.jar");
		Path generated = tmp.resolve("generated");

		// download version.json
		if (!Files.exists(versionJson)) {
			MinecraftVersionFile.download(version, versionJson);
		}
		MinecraftVersionFile versionFile = MinecraftVersionFile.load(versionJson);

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
