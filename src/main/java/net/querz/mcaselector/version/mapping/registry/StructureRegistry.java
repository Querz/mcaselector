package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class StructureRegistry {

	private StructureRegistry() {}

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private static final Set<String> valid = new HashSet<>();
	private static final Map<String, Set<String>> alts = new HashMap<>();
	static {
		FileHelper.loadFromResource("mapping/registry/structures.json", r -> {
			Set<String> set = GSON.fromJson(r, new TypeToken<>(){});
			for (String s : set) {
				valid.add(s);
				if (!containsUpperCase(s)) {
					valid.add("minecraft:" + s);
				}
			}
			for (String name : valid) {
				alts.computeIfAbsent(name, k -> new HashSet<>()).add(name);
				if (containsUpperCase(name)) {
					Set<String> alt = alts.get(name);
					String lower = name.toLowerCase();
					if (valid.contains(lower)) {
						alt.add(lower);
						alts.computeIfAbsent(lower, k -> new HashSet<>()).add(name);
					}
					String lowerNamespace = "minecraft:" + lower;
					if (valid.contains(lowerNamespace)) {
						alt.add(lowerNamespace);
						alts.computeIfAbsent(lowerNamespace, k -> new HashSet<>()).add(name);
					}
				} else if (name.startsWith("minecraft:")) {
					alts.get(name).add(name.substring(10));
				} else {
					alts.get(name).add("minecraft:" + name);
				}
			}
			return null;
		});
	}

	private static boolean containsUpperCase(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isUpperCase(c)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isValidName(String name) {
		return valid.contains(name);
	}

	public static Set<String> getAlts(String name) {
		return alts.getOrDefault(name, Collections.singleton(name));
	}
}
