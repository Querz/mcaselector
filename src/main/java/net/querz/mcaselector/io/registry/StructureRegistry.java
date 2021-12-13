package net.querz.mcaselector.io.registry;

import net.querz.mcaselector.debug.Debug;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class StructureRegistry {

	private StructureRegistry() {}

	private static final Map<String, String> lowerToHigher = new HashMap<>();
	private static final Map<String, String> higherToLower = new HashMap<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(StructureRegistry.class.getClassLoader().getResourceAsStream("mapping/all_structures.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				lowerToHigher.put(line.toLowerCase(), line);
				higherToLower.put(line, line.toLowerCase());
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_structures.txt", ex);
		}
	}

	public static boolean isValidName(String name) {
		return lowerToHigher.containsKey(name) || higherToLower.containsKey(name);
	}

	public static String getAltName(String name) {
		if (lowerToHigher.containsKey(name)) {
			return lowerToHigher.get(name);
		} else if (higherToLower.containsKey(name)) {
			return higherToLower.get(name);
		}
		return null;
	}
}
