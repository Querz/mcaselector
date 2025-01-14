package net.querz.mcaselector.version.mapping.color;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class ColorMapping {

	private static final StateColors missing = new SingleStateColors(BlockColor.MISSING) {
		@Override
		public boolean hasColor(BitSet state) {
			return false;
		}
	};

	public final Map<String, StateColors> colors;

	public ColorMapping() {
		colors = new HashMap<>();
	}

	private ColorMapping(Map<String, StateColors> colors) {
		this.colors = colors;
	}

	public BlockColor getBlockColor(String name, BitSet state) {
		return colors.getOrDefault(name, missing).getColor(state);
	}

	public boolean hasBlockColor(String name, BitSet state) {
		return colors.containsKey(name) && colors.get(name).hasColor(state);
	}

	public boolean hasBlockStateColors(String name) {
		StateColors c;
		return (c = colors.get(name)) != null && c instanceof BlockStateColors;
	}

	public void addBlockColor(String name, BitSet state, BlockColor color) {
		if (colors.containsKey(name)) {
			StateColors stateColors = colors.get(name);
			stateColors.setColor(state, color);
		} else if (state == null) {
			colors.put(name, new SingleStateColors(color));
		} else {
			colors.put(name, new BlockStateColors(state, color));
		}
	}

	public void compress() {
		for (Map.Entry<String, StateColors> entry : colors.entrySet()) {
			StateColors stateColors = entry.getValue();
			if (stateColors instanceof BlockStateColors blockStateColors) {
				StateColors compressedColors = blockStateColors.compress();
				colors.put(entry.getKey(), compressedColors);
			}
		}
	}

	public static class ColorMappingTypeAdapterFactory implements TypeAdapterFactory {

		private static final TypeAdapterFactory instance = new ColorMappingTypeAdapterFactory();

		private ColorMappingTypeAdapterFactory() {}

		public static TypeAdapterFactory getColorMappingTypeAdapterFactory() {
			return instance;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			if (type.getRawType() != ColorMapping.class) {
				return null;
			}
			return (TypeAdapter<T>) new ColorMappingTypeAdapter(gson);
		}

		private static class ColorMappingTypeAdapter extends TypeAdapter<ColorMapping> {

			private final Gson gson;
			private final TypeAdapter<StateColors> adapter;

			public ColorMappingTypeAdapter(Gson gson) {
				this.gson = gson;
				adapter = StateColors.StateColorsTypeAdapterFactory.getStateColorsTypeAdapterFactory()
						.create(gson, StateColors.StateColorsTypeAdapterFactory.token);
			}

			@Override
			public void write(JsonWriter out, ColorMapping value) throws IOException {
				out.beginObject();
				for (Map.Entry<String, StateColors> entry : value.colors.entrySet()) {
					out.name(entry.getKey());
					gson.toJson(entry.getValue(), entry.getValue().getClass(), out);
				}
				out.endObject();
			}

			@Override
			public ColorMapping read(JsonReader in) throws IOException {
				Map<String, StateColors> colors = new HashMap<>();
				in.beginObject();
				while (in.hasNext()) {
					String name = in.nextName();
					StateColors color = adapter.read(in);
					colors.put(name, color);
				}
				in.endObject();
				return new ColorMapping(colors);
			}
		}
	}
}
