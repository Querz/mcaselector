package net.querz.mcaselector.io.registry;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.BiomeFilter;
import net.querz.mcaselector.text.TextHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BiomeRegistry {

	private BiomeRegistry() {}

	private static final Map<Integer, String> idMapping = new HashMap<>();
	private static final Map<String, Integer> nameMapping = new HashMap<>();
	private static final Set<String> mapping = new HashSet<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/biome_name_to_id.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] split = line.split(";");
				if (split.length != 2) {
					Debug.dumpf("invalid biome mapping: %s", line);
					continue;
				}
				Integer id = TextHelper.parseInt(split[1], 10);
				if (id == null) {
					Debug.dumpf("invalid biome id: %s", line);
					continue;
				}
				idMapping.put(id, "minecraft:" + split[0]);
				nameMapping.put("minecraft:" + split[0], id);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/biome_name_to_id.txt", ex);
		}

		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/all_biome_names.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				mapping.add("minecraft:" + line);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_biome_names.txt", ex);
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

		public BiomeIdentifier(String name, int id) {
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
