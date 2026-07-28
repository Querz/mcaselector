package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.querz.mcaselector.version.mapping.minecraft.Blocks;
import net.querz.mcaselector.version.mapping.minecraft.ServerVersion;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Generates the UI-only block-state catalog from Mojang's server data-generator reports. */
public final class BlockStateCatalogConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private BlockStateCatalogConfig() {}

	public static Path generate(String minecraftVersion, Path temporaryDirectory) throws IOException {
		Path reports = temporaryDirectory.resolve("generated/reports/blocks.json");
		Path serverJar = temporaryDirectory.resolve("server.jar");
		Blocks blocks = Blocks.load(reports);
		int dataVersion;
		try (FileSystem fileSystem = FileSystems.newFileSystem(serverJar)) {
			dataVersion = ServerVersion.load(fileSystem.getPath("version.json")).worldVersion();
		}
		Path output = temporaryDirectory.resolve("configs").resolve(fileName(minecraftVersion));
		Files.writeString(output, toJson(minecraftVersion, dataVersion, blocks));
		return output;
	}

	static String toJson(String minecraftVersion, int dataVersion, Blocks blocks) {
		Map<String, CatalogBlock> catalogBlocks = new TreeMap<>();
		blocks.states.forEach((name, block) -> {
			Map<String, List<String>> properties = new TreeMap<>();
			block.properties().forEach((property, values) -> properties.put(property,
					values.stream().sorted().toList()));
			Map<String, String> defaults = new TreeMap<>();
			if (block.states != null) {
				block.states.stream()
						.filter(Blocks.Block.State::defaultState)
						.findFirst()
						.ifPresent(state -> defaults.putAll(state.properties()));
			}
			catalogBlocks.put(name, new CatalogBlock(properties, defaults, block.states == null ? 0 : block.states.size()));
		});
		return GSON.toJson(new CatalogData(minecraftVersion, dataVersion,
				"Mojang server reports/blocks.json generated from " + minecraftVersion + " server.jar", catalogBlocks));
	}

	public static String fileName(String minecraftVersion) {
		return "java_" + minecraftVersion.toLowerCase().replaceAll("[^a-z0-9]+", "_") + ".json";
	}

	private record CatalogData(String version, int dataVersion, String source, Map<String, CatalogBlock> blocks) {}

	private record CatalogBlock(Map<String, List<String>> properties, Map<String, String> defaultProperties,
			int stateCount) {}
}
