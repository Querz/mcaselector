package net.querz.mcaselector.version.mapping.blockstate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

public final class BlockStateCatalog {

	private static final Gson GSON = new GsonBuilder().create();
	private static final Logger LOGGER = LogManager.getLogger(BlockStateCatalog.class);
	private static final String CATALOG_INDEX_RESOURCE = "mapping/block_states/catalogs.json";
	private static final BlockStateCatalog UNAVAILABLE = new BlockStateCatalog("unavailable", 0,
			"No bundled block-state catalog is available", Collections.emptyMap());
	private static final List<BlockStateCatalog> AVAILABLE = loadAvailable();

	private final String version;
	private final int dataVersion;
	private final String source;
	private final Map<String, Block> blocks;

	private BlockStateCatalog(String version, int dataVersion, String source, Map<String, Block> blocks) {
		this.version = version;
		this.dataVersion = dataVersion;
		this.source = source;
		this.blocks = Collections.unmodifiableMap(blocks);
	}

	public static BlockStateCatalog load(Reader reader) {
		CatalogData data = GSON.fromJson(reader, new TypeToken<CatalogData>() {}.getType());
		if (data == null || data.version == null || data.source == null || data.blocks == null) {
			throw new IllegalArgumentException("invalid block-state catalog");
		}
		Map<String, Block> blocks = new TreeMap<>();
		data.blocks.forEach((name, block) -> blocks.put(normalizeName(name), block.normalized()));
		return new BlockStateCatalog(data.version, data.dataVersion, data.source, blocks);
	}

	static List<BlockStateCatalog> loadCatalogs(Reader index, Function<String, Reader> resourceLoader) {
		List<String> resourcePaths;
		try {
			resourcePaths = GSON.fromJson(index, new TypeToken<List<String>>() {}.getType());
		} catch (RuntimeException ex) {
			LOGGER.warn("failed to load block-state catalog index", ex);
			return List.of();
		}
		if (resourcePaths == null) {
			return List.of();
		}
		List<BlockStateCatalog> catalogs = new ArrayList<>();
		Set<String> versions = new HashSet<>();
		Set<Integer> dataVersions = new HashSet<>();
		for (String path : resourcePaths) {
			if (path == null || path.isBlank()) {
				continue;
			}
			try (Reader resource = resourceLoader.apply(path)) {
				if (resource == null) {
					LOGGER.warn("missing block-state catalog resource {}", path);
					continue;
				}
				BlockStateCatalog catalog = load(resource);
				if (!versions.add(catalog.version()) || !dataVersions.add(catalog.dataVersion())) {
					LOGGER.warn("skipping duplicate block-state catalog {} ({})", catalog.version(), catalog.dataVersion());
					continue;
				}
				catalogs.add(catalog);
			} catch (Exception ex) {
				LOGGER.warn("failed to load block-state catalog resource {}", path, ex);
			}
		}
		catalogs.sort(Comparator.comparingInt(BlockStateCatalog::dataVersion));
		return List.copyOf(catalogs);
	}

	private static List<BlockStateCatalog> loadAvailable() {
		InputStream index = BlockStateCatalog.class.getClassLoader().getResourceAsStream(CATALOG_INDEX_RESOURCE);
		if (index == null) {
			LOGGER.warn("missing block-state catalog index {}", CATALOG_INDEX_RESOURCE);
			return List.of();
		}
		try (Reader reader = new InputStreamReader(index, StandardCharsets.UTF_8)) {
			return loadCatalogs(reader, path -> {
				InputStream resource = BlockStateCatalog.class.getClassLoader().getResourceAsStream(path);
				return resource == null ? null : new InputStreamReader(resource, StandardCharsets.UTF_8);
			});
		} catch (Exception ex) {
			LOGGER.warn("failed to load block-state catalogs", ex);
			return List.of();
		}
	}

	public static List<BlockStateCatalog> available() {
		return AVAILABLE;
	}

	public static BlockStateCatalog latestJava() {
		return AVAILABLE.isEmpty() ? UNAVAILABLE : AVAILABLE.getLast();
	}

	public static Optional<BlockStateCatalog> findByVersion(String version) {
		return AVAILABLE.stream()
				.filter(catalog -> catalog.version().equals(version))
				.findFirst();
	}

	public String version() {
		return version;
	}

	public int dataVersion() {
		return dataVersion;
	}

	public String source() {
		return source;
	}

	public Set<String> blockNames() {
		return blocks.keySet();
	}

	public boolean containsBlock(String blockName) {
		return blocks.containsKey(normalizeName(blockName));
	}

	public Optional<Block> getBlock(String blockName) {
		return Optional.ofNullable(blocks.get(normalizeName(blockName)));
	}

	public Map<String, List<String>> properties(String blockName) {
		return getBlock(blockName)
				.map(Block::properties)
				.orElseGet(Collections::emptyMap);
	}

	public Map<String, String> defaultProperties(String blockName) {
		return getBlock(blockName)
				.map(Block::defaultProperties)
				.orElseGet(Collections::emptyMap);
	}

	public boolean hasProperty(String blockName, String propertyName) {
		return properties(blockName).containsKey(propertyName);
	}

	public boolean isValidPropertyValue(String blockName, String propertyName, String value) {
		List<String> values = properties(blockName).get(propertyName);
		return values != null && values.contains(value);
	}

	public static String normalizeName(String blockName) {
		if (blockName == null || blockName.isBlank()) {
			return "";
		}
		String trimmed = blockName.trim();
		if (trimmed.indexOf(':') < 0) {
			return "minecraft:" + trimmed;
		}
		return trimmed;
	}

	public record Block(
			Map<String, List<String>> properties,
			Map<String, String> defaultProperties,
			int stateCount) {

		private Block normalized() {
			Map<String, List<String>> normalizedProperties = new TreeMap<>();
			(properties == null ? Collections.<String, List<String>>emptyMap() : properties)
					.forEach((name, values) -> normalizedProperties.put(name, values == null ? List.of() : List.copyOf(values)));
			return new Block(
					Collections.unmodifiableMap(normalizedProperties),
					Collections.unmodifiableMap(new TreeMap<>(defaultProperties == null ? Collections.emptyMap() : defaultProperties)),
					stateCount);
		}
	}

	private record CatalogData(
			String version,
			int dataVersion,
			String source,
			Map<String, Block> blocks) {}
}
