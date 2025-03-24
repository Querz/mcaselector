package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.version.mapping.minecraft.*;
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

public class BiomeConfig {

	private final Set<String> biomes;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeHierarchyAdapter(Set.class, new CollectionAdapter())
			.create();

	public BiomeConfig() {
		this.biomes = new HashSet<>();
	}

	public BiomeConfig(Set<String> biomes) {
		this.biomes = biomes;
	}

	public static BiomeConfig load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return load(reader);
		}
	}

	public static BiomeConfig load(Reader reader) throws IOException {
		return new BiomeConfig(GSON.fromJson(reader, new TypeToken<>() {}));
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(biomes);
		Files.writeString(path, json);
	}

	public void merge(BiomeConfig other) {
		biomes.addAll(other.biomes);
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

		// load blocks.json
		Path biomes = generated.resolve("data/minecraft/worldgen/biome");
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(biomes)) {
			for (Path b : ds) {
				if (!Files.isRegularFile(b)) {
					continue;
				}
				this.biomes.add("minecraft:" + b.getFileName().toString().substring(0, b.getFileName().toString().lastIndexOf('.')));
			}
		}
	}
}
