package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class BiomeRegistry {

	private BiomeRegistry() {}

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private static final Map<String, Integer> nameMapping = FileHelper.loadFromResource(
			"mapping/registry/biome_name_to_id.json",
			r -> GSON.fromJson(r, new TypeToken<Map<String, Integer>>(){}.getType()));
	private static final Map<Integer, String> idMapping = new HashMap<>();
	static {
		nameMapping.forEach((k, v) -> idMapping.put(v, k));
	}
	private static final Set<String> mapping = FileHelper.loadFromResource(
			"mapping/registry/biomes.json",
			r -> GSON.fromJson(r, new TypeToken<Set<String>>(){}.getType()));

	public static boolean isValidName(String name) {
		return mapping.contains(name);
	}

	public static boolean isValidID(int id) {
		return idMapping.containsKey(id);
	}

	public static String toName(int id) {
		return idMapping.get(id);
	}

	public static Integer toID(String name) {
		return nameMapping.getOrDefault(name, null);
	}

	public static class BiomeIdentifier implements Serializable {

		Integer id;
		String name;

		public BiomeIdentifier(String name) {
			this(name, toID(name));
		}

		public BiomeIdentifier(int id) {
			this(toName(id), id);
		}

		public BiomeIdentifier(String name, Integer id) {
			this.id = id;
			this.name = name;
		}

		public int getID() {
			return id;
		}

		public String getName() {
			return name;
		}

		public boolean matches(String name) {
			return this.name != null && this.name.equals(name);
		}

		public boolean matches(int id) {
			return this.id == id;
		}

		@Override
		public String toString() {
			if (name != null) {
				if (isValidName(name)) {
					return name;
				} else {
					return "'" + name + "'";
				}
			} else if (id != null) {
				if (isValidID(id)) {
					return Integer.toString(id);
				} else {
					return "'" + id + "'";
				}
			}
			return "";
		}
	}
}
