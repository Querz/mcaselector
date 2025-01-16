package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersionFile;
import net.querz.mcaselector.version.mapping.minecraft.Registries;
import net.querz.mcaselector.version.mapping.minecraft.Report;
import net.querz.mcaselector.version.mapping.util.Download;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class EntityConfig {

	@SerializedName("entities") private Set<String> entities;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(EntityConfig.class, new EntityConfigTypeAdapter())
			.create();

	public EntityConfig() {
		this.entities = new HashSet<>();
	}

	public EntityConfig(Set<String> entities) {
		this.entities = entities;
	}

	public static EntityConfig load(Path path) throws IOException {
		return GSON.fromJson(Files.newBufferedReader(path), EntityConfig.class);
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(this);
		Files.writeString(path, json);
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
		entities.addAll(registries.entityType().entries().keySet());
	}

	public static class EntityConfigTypeAdapter extends TypeAdapter<EntityConfig> {

		@Override
		public void write(JsonWriter out, EntityConfig value) throws IOException {
			out.beginArray();
			for (String e : value.entities) {
				out.value(e);
			}
			out.endArray();
		}

		@Override
		public EntityConfig read(JsonReader in) throws IOException {
			Set<String> entities = new HashSet<>();
			in.beginArray();
			while (in.hasNext()) {
				entities.add(in.nextString());
			}
			in.endArray();
			return new EntityConfig(entities);
		}
	}
}
