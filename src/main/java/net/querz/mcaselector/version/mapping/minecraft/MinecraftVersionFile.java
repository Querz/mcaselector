package net.querz.mcaselector.version.mapping.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MinecraftVersionFile {

	@SerializedName("downloads")
	private Downloads downloads;
	private static final Gson GSON = new GsonBuilder().create();

	private MinecraftVersionFile() {}

	public static MinecraftVersionFile load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return GSON.fromJson(reader, MinecraftVersionFile.class);
		}
	}

	public static void download(MinecraftVersion minecraftVersion, Path output) throws IOException {
		net.querz.mcaselector.version.mapping.util.Download.to(minecraftVersion.url(), output);
	}

	public Downloads getDownloads() {
		return downloads;
	}

	public record Downloads(
			@SerializedName("client") Download client,
			@SerializedName("client_mappings") Download clientMappings,
			@SerializedName("server") Download server,
			@SerializedName("server_mappings") Download serverMappings) {}

	public record Download(
			@SerializedName("sha1") String sha1,
			@SerializedName("size") int size,
			@SerializedName("url") String url) {}

}
