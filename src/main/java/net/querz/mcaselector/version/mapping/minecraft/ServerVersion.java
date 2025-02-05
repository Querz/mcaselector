package net.querz.mcaselector.version.mapping.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// the version.json file inside the server.jar
public record ServerVersion (
		@SerializedName("id") String id,
		@SerializedName("name") String name,
		@SerializedName("world_version") int worldVersion, // DataVersion
		@SerializedName("series_id") String seriesId,
		@SerializedName("protocol_version") int protocolVersion,
		@SerializedName("build_time") String buildTime,
		@SerializedName("java_component") String javaComponent,
		@SerializedName("java_version") String javaVersion,
		@SerializedName("stable") boolean stable,
		@SerializedName("use_editor") boolean useEditor) {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	public static ServerVersion load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return GSON.fromJson(reader, ServerVersion.class);
		}
	}
}
