package net.querz.mcaselector.version.mapping.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.Collection;

public class CollectionAdapter implements JsonSerializer<Collection<?>> {

	@Override
	public JsonElement serialize(Collection<?> src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null || src.isEmpty()) {
			return null;
		}
		JsonArray array = new JsonArray();
		for (Object o : src) {
			array.add(context.serialize(o));
		}
		new JsonArrayList(array).sort((a, b) -> {
			if (a.isJsonPrimitive() && b.isJsonPrimitive()) {
				return a.getAsString().compareTo(b.getAsString());
			}
			return 0;
		});
		return array;
	}

	private static class JsonArrayList extends AbstractList<JsonElement> {

		private final JsonArray jsonArray;

		private JsonArrayList(JsonArray jsonArray) {
			this.jsonArray = jsonArray;
		}

		@Override
		public JsonElement get(int index) {
			return jsonArray.get(index);
		}

		@Override
		public JsonElement set(int index, JsonElement element) {
			return jsonArray.set(index, element);
		}

		@Override
		public int size() {
			return jsonArray.size();
		}
	}
}
