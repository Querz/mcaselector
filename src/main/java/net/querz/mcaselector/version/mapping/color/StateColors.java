package net.querz.mcaselector.version.mapping.color;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;

public interface StateColors extends Iterable<Map.Entry<BitSet, BlockColor>> {

	BlockColor getColor(BitSet state);

	BlockColor getDefaultColor();

	boolean hasColor(BitSet state);

	void setColor(BitSet state, BlockColor color);

	class StateColorsTypeAdapterFactory implements TypeAdapterFactory {

		private static final TypeAdapterFactory instance = new StateColorsTypeAdapterFactory();

		public static final TypeToken<StateColors> token = new TypeToken<>(){};

		private StateColorsTypeAdapterFactory() {}

		public static TypeAdapterFactory getStateColorsTypeAdapterFactory() {
			return instance;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			if (type.getRawType() != StateColors.class) {
				return null;
			}
			return (TypeAdapter<T>) new StateColorsTypeAdapter(gson);
		}

		private static final class StateColorsTypeAdapter extends TypeAdapter<StateColors> {

			private final Gson gson;

			private StateColorsTypeAdapter(Gson gson) {
				this.gson = gson;
			}

			@Override
			public void write(JsonWriter out, StateColors value) {
				if (value instanceof SingleStateColors) {
					gson.toJson(value, SingleStateColors.class, out);
				} else {
					gson.toJson(value, BlockStateColors.class, out);
				}
			}

			@Override
			public StateColors read(JsonReader in) throws IOException {
				JsonElement jsonElement = gson.fromJson(in, JsonElement.class);
				if (jsonElement.isJsonObject() && !jsonElement.getAsJsonObject().has("color")) {
					return gson.fromJson(jsonElement, BlockStateColors.class);
				} else {
					return gson.fromJson(jsonElement, SingleStateColors.class);
				}
			}
		}
	}
}
