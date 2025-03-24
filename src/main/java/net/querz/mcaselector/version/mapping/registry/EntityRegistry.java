package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityRegistry {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private static final Map<String, String> valid = FileHelper.loadFromResource("mapping/registry/entities.json", r -> {
		Map<String, String> map = new HashMap<>();
		List<String> status = GSON.fromJson(r, new TypeToken<List<String>>(){}.getType());
		for (String s : status) {
			map.put(s, "minecraft:" + s);
			map.put("minecraft:" + s, s);
		}
		return map;
	});

	public static boolean isValidName(String name) {
		return valid.containsKey(name) || name != null && name.startsWith("'") && name.endsWith("'");
	}
}
