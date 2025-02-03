package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.querz.mcaselector.version.mapping.minecraft.Blocks;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersionFile;
import net.querz.mcaselector.version.mapping.minecraft.Report;
import net.querz.mcaselector.version.mapping.util.Download;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class BlockConfig {

	private final Set<String> blocks;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(BlockConfig.class, new BlockConfigTypeAdapter())
			.create();

	public BlockConfig() {
		blocks = new HashSet<>();
	}

	public BlockConfig(Set<String> blocks) {
		this.blocks = blocks;
	}

	public static BlockConfig load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return load(reader);
		}
	}

	public static BlockConfig load(Reader reader) throws IOException {
		return GSON.fromJson(reader, BlockConfig.class);
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(this);
		Files.writeString(path, json);
	}

	public void merge(BlockConfig other) {
		blocks.addAll(other.blocks);
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
		Path blocksJson = generated.resolve("reports/blocks.json");
		Blocks blocks = Blocks.load(blocksJson);

		blocks.states.keySet().forEach(k -> this.blocks.add(k.substring(10)));
	}

	public static class BlockConfigTypeAdapter extends TypeAdapter<BlockConfig> {

		@Override
		public void write(JsonWriter out, BlockConfig value) throws IOException {
			out.beginArray();
			TreeSet<String> sorted = new TreeSet<>(value.blocks);
			for (String e : sorted) {
				out.value(e);
			}
			out.endArray();
		}

		@Override
		public BlockConfig read(JsonReader in) throws IOException {
			Set<String> blocks = new HashSet<>();
			in.beginArray();
			while (in.hasNext()) {
				blocks.add(in.nextString());
			}
			in.endArray();
			return new BlockConfig(blocks);
		}
	}
}
