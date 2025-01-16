package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.filter.filters.BiomeFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BiomeRegistry {

	private static final Logger LOGGER = LogManager.getLogger(BiomeRegistry.class);

	private BiomeRegistry() {}

	private static final Map<Integer, String> idMapping = new HashMap<>();
	private static final Map<String, Integer> nameMapping = new HashMap<>();
	private static final Set<String> mapping = new HashSet<>();

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	static {
		try (BufferedReader bis = new BufferedReader(new InputStreamReader(
				Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/registry/biome_name_to_id.json"))))) {
			Map<String, Integer> biomeToID = GSON.fromJson(bis, new TypeToken<Map<String, Integer>>(){}.getType());
			for (Map.Entry<String, Integer> entry : biomeToID.entrySet()) {
				idMapping.put(entry.getValue(), "minecraft:" + entry.getKey());
				nameMapping.put("minecraft:" + entry.getKey(), entry.getValue());
			}
		} catch (IOException ex) {
			LOGGER.error("error reading mapping/registry/biome_name_to_id.json", ex);
		}
		try (BufferedReader bis = new BufferedReader(new InputStreamReader(
				Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/registry/biome.json"))))) {
			List<String> status = GSON.fromJson(bis, new TypeToken<List<String>>(){}.getType());
			for (String s : status) {
				mapping.add("minecraft:" + s);
			}
		} catch (IOException ex) {
			LOGGER.error("error reading mapping/all_biome_names.txt", ex);
		}
	}

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

	public static class BiomeIdentifier {

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
