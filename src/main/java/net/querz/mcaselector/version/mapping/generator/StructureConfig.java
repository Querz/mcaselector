package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
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

	private final Set<StructureData> structures;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeHierarchyAdapter(Set.class, new CollectionAdapter())
			.registerTypeAdapter(StructureData.class, new StructureDataTypeAdapter())
			.create();

	public StructureConfig() {
		structures = new HashSet<>();
	}

	public StructureConfig(Set<StructureData> structures) {
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
		for (StructureData os : other.structures) {
			structures.removeIf(ts -> os.name.equals(ts.name) || os.alt != null && os.alt.contains(ts.name));
		}
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
				structures.add(new StructureData(name, null, null, "", null));
			}
		}
	}

	public record StructureData (
			String name,
			Set<String> alt,
			@SerializedName("max_scale") Float maxScale,
			String icon,
			String display) {

		public String[] allNames() {
			if (alt == null) {
				return new String[] { name };
			}
			String[] res = new String[alt.size() + 1];
			alt.toArray(res);
			res[res.length - 1] = name;
			return res;
		}

		@Override
		public Float maxScale() {
			return maxScale == null ? Float.MAX_VALUE : maxScale;
		}

		@Override
		public String display() {
			return display == null ? name : display;
		}
	}

	public static class StructureDataTypeAdapter extends TypeAdapter<StructureData> {

		@Override
		public void write(JsonWriter out, StructureData value) throws IOException {
			out.beginObject();
			out.name("name").value(value.name);
			if (value.alt != null) {
				out.name("alt").beginArray();
				for (String a : value.alt()) {
					out.value(a);
				}
				out.endArray();
			}
			if (value.maxScale != null) {
				out.name("max_scale").value(value.maxScale);
			}
			out.name("icon").value(value.icon);
			if (value.display != null) {
				out.name("display").value(value.display);
			}
			out.endObject();
		}

		@Override
		public StructureData read(JsonReader in) throws IOException {
			String name = "", icon = "", display = "";
			Set<String> alt = null;
			Float maxScale = null;
			in.beginObject();
			while (in.peek() != JsonToken.END_OBJECT) {
				switch (in.nextName()) {
					case "name" -> name = in.nextString();
					case "icon" -> icon = in.nextString();
					case "display" -> display = in.nextString();
					case "max_scale" -> maxScale = (float) in.nextDouble();
					case "alt" -> {
						alt = new HashSet<>();
						in.beginArray();
						while (in.peek() != JsonToken.END_ARRAY) {
							alt.add(in.nextString());
						}
						in.endArray();
					}
				}
			}
			in.endObject();
			return new StructureData(name, alt, maxScale, icon, display);
		}
	}
}
