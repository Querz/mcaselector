package net.querz.mcaselector.version.mapping.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.version.mapping.color.BlockStates;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// representation of blocks.json
public class Blocks {

	public Map<String, Block> states;

	public static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	public static Blocks load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			Blocks blocks = new Blocks();
			blocks.states = GSON.fromJson(reader, new TypeToken<Map<String, Block>>(){}.getType());
			return blocks;
		}
	}

	public BlockStates generateBlockStates() {
		Map<String, Set<String>> properties = parseProperties();
		Map<String, Map<String, Integer>> indexedProperties = new TreeMap<>();
		int index = 0;
		for (Map.Entry<String, Set<String>> entry : properties.entrySet()) {
			Map<String, Integer> indexedValues = new HashMap<>();
			for (String value : entry.getValue()) {
				indexedValues.put(value, index++);
			}
			indexedProperties.put(entry.getKey(), indexedValues);
		}
		return new BlockStates(indexedProperties, index - 1);
	}

	// returns a TreeMap containing all possible block state properties ordered by their occurrence,
	// from highest to lowest frequency.
	private TreeMap<String, Set<String>> parseProperties() {
		Map<String, Integer> frequencies = new HashMap<>();
		Map<String, Set<String>> properties = new HashMap<>();
		for (Map.Entry<String, Block> e : states.entrySet()) {
			for (Map.Entry<String, Set<String>> property : e.getValue().properties().entrySet()) {
				properties.computeIfAbsent(property.getKey(), k -> new HashSet<>()).addAll(property.getValue());
				frequencies.compute(property.getKey(), (k, v) -> v == null ? 1 : v + 1);
			}
		}
		// sort by frequency
		TreeMap<String, Set<String>> ordered = new TreeMap<>(
				Comparator.comparingInt((String a) -> frequencies.get(a)).thenComparing(a -> a).reversed());
		ordered.putAll(properties);
		return ordered;
	}

	public static class Block {

		private Map<String, Set<String>> properties;
		public Set<State> states;

		public Map<String, Set<String>> properties() {
			return properties == null ? Collections.emptyMap() : properties;
		}

		public static class State {

			public int id;
			private Map<String, String> properties;

			public Map<String, String> properties() {
				return properties == null ? Collections.emptyMap() : properties;
			}
		}
	}
}
