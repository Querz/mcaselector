package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersionFile;
import net.querz.mcaselector.version.mapping.minecraft.Registries;
import net.querz.mcaselector.version.mapping.minecraft.Report;
import net.querz.mcaselector.version.mapping.util.CollectionAdapter;
import net.querz.mcaselector.version.mapping.util.Download;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class EntityConfig {

	private final Set<String> entities;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeHierarchyAdapter(Set.class, new CollectionAdapter())
			.create();

	public EntityConfig() {
		this.entities = new HashSet<>();
	}

	public EntityConfig(Set<String> entities) {
		this.entities = entities;
	}

	public static EntityConfig load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return load(reader);
		}
	}

	public static EntityConfig load(Reader reader) throws IOException {
		return new EntityConfig(GSON.fromJson(reader, new TypeToken<>() {}));
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(entities);
		Files.writeString(path, json);
	}

	public void merge(EntityConfig other) {
		entities.addAll(other.entities);
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

		Path registriesJson = generated.resolve("reports/registries.json");
		Registries registries = Registries.load(registriesJson);
		registries.entityType().entries().keySet().forEach(k -> entities.add(k.substring(10)));
	}
}
