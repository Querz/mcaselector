package net.querz.mcaselector.version.mapping.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public record Registries(
		@SerializedName("minecraft:entity_type") EntityType entityType) {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	public static Registries load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return GSON.fromJson(reader, Registries.class);
		}
	}

	public record EntityType(
			@SerializedName("default") String def,
			@SerializedName("entries") Map<String, Entry> entries,
			@SerializedName("protocol_id") int protocolID
	) {}

	public record Entry(
			@SerializedName("protocol_id") int protocolID
	) {}
}
