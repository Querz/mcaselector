package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.version.mapping.minecraft.Blocks;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersionFile;
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

public class BlockConfig {

	private final Set<String> blocks;

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeHierarchyAdapter(Set.class, new CollectionAdapter())
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
		return new BlockConfig(GSON.fromJson(reader, new TypeToken<>() {}));
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(blocks);
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
}
