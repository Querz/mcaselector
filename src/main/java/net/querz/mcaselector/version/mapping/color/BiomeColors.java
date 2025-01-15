package net.querz.mcaselector.version.mapping.color;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BiomeColors {

	public final Map<String, BiomeTints> biomes;

	public BiomeColors() {
		biomes = new HashMap<>();
	}

	public BiomeColors(Map<String, BiomeTints> biomes) {
		this.biomes = biomes;
	}

	public void addTints(String id, BiomeTints tints) {
		biomes.put(id, tints);
	}

	public BiomeTints getTints(String id) {
		return biomes.get(id);
	}

	public record BiomeTints (int grassColor, int foliageColor, int waterColor) {}

	public static class BiomeColorsTypeAdapter extends TypeAdapter<BiomeColors> {

		@Override
		public void write(JsonWriter out, BiomeColors value) throws IOException {
			out.beginObject();
			for (Map.Entry<String, BiomeTints> entry : value.biomes.entrySet()) {
				out.name(entry.getKey());
				out.beginArray();
				out.value(String.format("%06x", entry.getValue().grassColor()));
				out.value(String.format("%06x", entry.getValue().foliageColor()));
				out.value(String.format("%06x", entry.getValue().waterColor()));
				out.endArray();
			}
			out.endObject();
		}

		@Override
		public BiomeColors read(JsonReader in) throws IOException {
			Map<String, BiomeTints> biomes = new HashMap<>();
			in.beginObject();
			while (in.hasNext()) {
				String name = in.nextName();
				in.beginArray();
				int grassColor = Integer.parseInt(in.nextString(), 16);
				int foliageColor = Integer.parseInt(in.nextString(), 16);
				int waterColor = Integer.parseInt(in.nextString(), 16);
				biomes.put(name, new BiomeTints(grassColor, foliageColor, waterColor));
				in.endArray();
			}
			in.endObject();
			return new BiomeColors(biomes);
		}
	}
}
