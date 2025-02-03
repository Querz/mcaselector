package net.querz.mcaselector.version.mapping.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Collection;

public class OmitEmptyCollectionAdapter implements JsonSerializer<Collection<?>> {

	@Override
	public JsonElement serialize(Collection<?> src, Type typeOfSrc, JsonSerializationContext context) {
		if (src == null || src.isEmpty()) {
			return null;
		}
		JsonArray array = new JsonArray();
		for (Object o : src) {
			array.add(context.serialize(o));
		}
		return array;
	}
}
