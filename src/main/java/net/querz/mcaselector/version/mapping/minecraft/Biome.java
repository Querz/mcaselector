package net.querz.mcaselector.version.mapping.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Biome {

	@SerializedName("effects") public Effects effects;
	@SerializedName("temperature") public double temperature;
	@SerializedName("downfall") public double downfall;

	public static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	public static Biome load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return GSON.fromJson(reader, Biome.class);
		}
	}

	public record Effects (
			@SerializedName("grass_color") Integer grassTint,
			@SerializedName("foliage_color") Integer foliageTint,
			@SerializedName("water_color") Integer waterTint,
			@SerializedName("dry_foliage_color") Integer dryFoliageTint
	) {}
}
