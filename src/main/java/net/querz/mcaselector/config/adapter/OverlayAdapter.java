package net.querz.mcaselector.config.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.overlay.OverlayType;
import java.lang.reflect.Type;
import java.util.function.Function;

public class OverlayAdapter implements JsonSerializer<Overlay>, JsonDeserializer<Overlay> {

	@Override
	public JsonElement serialize(Overlay src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", src.getType().name());
		obj.addProperty("active", src.isActive());
		obj.addProperty("min", src.min());
		obj.addProperty("max", src.max());
		obj.addProperty("rawMin", src.getRawMin());
		obj.addProperty("rawMax", src.getRawMax());
		if (src.multiValues() != null) {
			JsonArray multiValues = new JsonArray();
			for (String multiValue : src.multiValues()) {
				multiValues.add(multiValue);
			}
			obj.add("multiValues", multiValues);
		}
		obj.addProperty("rawMultiValues", src.getRawMultiValues());
		obj.addProperty("minHue", src.getMinHue());
		obj.addProperty("maxHue", src.getMaxHue());
		src.writeCustomJSON(obj);
		return obj;
	}

	@Override
	public Overlay deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject obj = json.getAsJsonObject();
		OverlayType type = OverlayType.valueOf(obj.get("type").getAsString());
		Overlay overlay = type.instance();
		overlay.setActive(get(obj, "active", false, JsonElement::getAsBoolean));
		overlay.setMinInt(get(obj, "min", null, JsonElement::getAsInt));
		overlay.setMaxInt(get(obj, "max", null, JsonElement::getAsInt));
		overlay.setRawMin(get(obj, "rawMin", null, JsonElement::getAsString));
		overlay.setRawMax(get(obj, "rawMax", null, JsonElement::getAsString));
		JsonArray mv = get(obj, "multiValues", null, JsonElement::getAsJsonArray);
		if (mv != null) {
			String[] multiValues = new String[mv.size()];
			for (int i = 0; i < mv.size(); i++) {
				multiValues[i] = mv.get(i).getAsString();
			}
			overlay.setMultiValues(multiValues);
		}
		overlay.setRawMultiValues(get(obj, "rawMultiValues", null, JsonElement::getAsString));
		overlay.setMinHue(get(obj, "minHue", 0.0f, JsonElement::getAsFloat));
		overlay.setMaxHue(get(obj, "maxHue", 0.0f, JsonElement::getAsFloat));
		overlay.readCustomJSON(obj);
		return overlay;
	}

	public static <T> T get(JsonObject obj, String name, T def, Function<JsonElement, T> parser) {
		JsonElement e = obj.get(name);
		if (e == null || e.isJsonNull()) {
			return def;
		}
		return parser.apply(e);
	}
}
