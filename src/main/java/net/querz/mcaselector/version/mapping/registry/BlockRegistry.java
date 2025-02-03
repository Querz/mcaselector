package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BlockRegistry {

	private BlockRegistry() {}

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private static final Map<String, String> valid = FileHelper.loadFromResource("mapping/registry/blocks.json", r -> {
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

	public static String[] parseBlockNames(String raw) {
		List<String> blocks = new ArrayList<>();
		String[] split = raw.split(",");
		for (String s : split) {
			s = s.trim();
			if (s.length() >= 2 && s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\'') {
				blocks.add(s.substring(1, s.length() - 1));
			} if (valid.containsKey(s)) {
				if (s.startsWith("minecraft:")) {
					blocks.add(s);
				} else {
					blocks.add(valid.get(s));
				}
			}
		}
		if (blocks.isEmpty()) {
			return null;
		}
		return blocks.toArray(new String[0]);
	}
}
