package net.querz.mcaselector.io.registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public final class StructureRegistry {

	private static final Logger LOGGER = LogManager.getLogger(StructureRegistry.class);

	private StructureRegistry() {}

	private static final Set<String> valid = new HashSet<>();

	private static final Map<String, Set<String>> alts = new HashMap<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(StructureRegistry.class.getClassLoader().getResourceAsStream("mapping/all_structures.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				valid.add(line);
				if (!containsUpperCase(line)) {
					valid.add("minecraft:" + line);
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
		} catch (IOException ex) {
			LOGGER.error("error reading mapping/all_structures.txt", ex);
		}
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
