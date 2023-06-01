package net.querz.mcaselector.config.adapter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.overlay.OverlayType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

public class OverlayAdapter extends TypeAdapter<Overlay> {

	@Override
	public void write(JsonWriter out, Overlay value) throws IOException {
		out.beginObject();
		out.name("type").value(value.getType().name());
		out.name("active").value(value.isActive());
		out.name("min").value(value.min());
		out.name("max").value(value.max());
		out.name("rawMin").value(value.getRawMin());
		out.name("rawMax").value(value.getRawMax());
		out.name("multiValues");
		if (value.multiValues() != null) {
			out.beginArray();
			for (String multiValue : value.multiValues()) {
				out.value(multiValue);
			}
			out.endArray();
		} else {
			out.nullValue();
		}
		out.name("rawMultiValues").value(value.getRawMultiValues());
		out.name("minHue").value(value.getMinHue());
		out.name("maxHue").value(value.getMaxHue());
		value.writeCustomJSON(out);
		out.endObject();
	}

	@Override
	public Overlay read(JsonReader in) throws IOException {
		Gson gson = new Gson();
		Map<String, Object> map = gson.fromJson(in, Map.class);
		OverlayType type = OverlayType.valueOf((String) map.get("type"));
		Overlay overlay = type.instance();
		overlay.setActive(get(map, "active", false, v -> (Boolean) v));
		overlay.setMinInt(get(map, "min", null, v -> Integer.parseInt((String) v)));
		overlay.setMaxInt(get(map, "max", null, v -> Integer.parseInt((String) v)));
		overlay.setRawMin(get(map, "rawMin", null, v -> (String) v));
		overlay.setRawMax(get(map, "rawMax", null, v -> (String) v));
		overlay.setMultiValues(get(map, "multiValues", null, v -> {
			ArrayList<Object> list = (ArrayList<Object>) v;
			String[] multiValues = new String[list.size()];
			for (int i = 0; i < list.size(); i++) {
				multiValues[i] = (String) list.get(i);
			}
			return multiValues;
		}));
		overlay.setRawMultiValues(get(map, "rawMultiValues", null, v -> (String) v));
		overlay.setMinHue(get(map, "minHue", 0.0f, v -> Float.parseFloat((String) v)));
		overlay.setMaxHue(get(map, "maxHue", 0.0f, v -> Float.parseFloat((String) v)));
		overlay.readCustomJSON(map);
		return null;
	}

	private <T> T get(Map<String, Object> map, String name, T def, Function<Object, T> parser) {
		if (!map.containsKey(name)) {
			return def;
		}
		Object o = map.get(name);
		if (o == null) {
			return def;
		}
		return parser.apply(o);
	}
}
