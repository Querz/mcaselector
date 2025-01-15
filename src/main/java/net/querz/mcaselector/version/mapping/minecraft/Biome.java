package net.querz.mcaselector.version.mapping.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
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

	public Biome(Path path) throws IOException {
		Biome tmp = GSON.fromJson(Files.newBufferedReader(path), Biome.class);
		effects = tmp.effects;
		temperature = tmp.temperature;
		downfall = tmp.downfall;
	}

	public record Effects (
			@SerializedName("grass_color") Integer grassTint,
			@SerializedName("foliage_color") Integer foliageTint,
			@SerializedName("water_color") Integer waterTint
	) {}
}
