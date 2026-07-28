package net.querz.mcaselector.version.mapping.blockstate;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStateCatalogTest {

	@Test
	void loadsJavaOneTwentyOneNineCatalog() {
		BlockStateCatalog catalog = BlockStateCatalog.findByVersion("1.21.9").orElseThrow();

		assertEquals("1.21.9", catalog.version());
		assertEquals(4554, catalog.dataVersion());
		assertTrue(catalog.blockNames().contains("minecraft:acacia_trapdoor"));
		assertTrue(catalog.blockNames().contains("minecraft:blue_ice"));
	}

	@Test
	void bundlesReleaseMilestoneCatalogsAndDefaultsToTheNewest() {
		List<BlockStateCatalog> catalogs = BlockStateCatalog.available();

		assertEquals(List.of("1.18.2", "1.20.6", "1.21.9", "1.21.11", "26.2"),
				catalogs.stream().map(BlockStateCatalog::version).toList());
		assertEquals(List.of(2975, 3839, 4554, 4671, 4903),
				catalogs.stream().map(BlockStateCatalog::dataVersion).toList());
		assertEquals("26.2", BlockStateCatalog.latestJava().version());
	}

	@Test
	void milestoneCatalogsExposeVersionSpecificBlocks() {
		BlockStateCatalog oneEighteen = BlockStateCatalog.findByVersion("1.18.2").orElseThrow();
		BlockStateCatalog oneTwenty = BlockStateCatalog.findByVersion("1.20.6").orElseThrow();
		BlockStateCatalog oneTwentyOne = BlockStateCatalog.findByVersion("1.21.9").orElseThrow();
		BlockStateCatalog twentySix = BlockStateCatalog.findByVersion("26.2").orElseThrow();

		assertFalse(oneEighteen.containsBlock("minecraft:mangrove_planks"));
		assertTrue(oneTwenty.containsBlock("minecraft:mangrove_planks"));
		assertFalse(oneTwenty.containsBlock("minecraft:resin_block"));
		assertTrue(oneTwentyOne.containsBlock("minecraft:resin_block"));
		assertTrue(twentySix.containsBlock("minecraft:sulfur"));
	}

	@Test
	void exposesPropertiesForStatefulBlocks() {
		BlockStateCatalog catalog = BlockStateCatalog.latestJava();

		assertTrue(catalog.containsBlock("acacia_trapdoor"));
		assertTrue(catalog.hasProperty("minecraft:acacia_trapdoor", "facing"));
		assertTrue(catalog.isValidPropertyValue("minecraft:acacia_trapdoor", "facing", "north"));
		assertTrue(catalog.isValidPropertyValue("minecraft:acacia_trapdoor", "open", "false"));
		assertFalse(catalog.isValidPropertyValue("minecraft:acacia_trapdoor", "facing", "up"));

		assertEquals(Map.of(
				"facing", "north",
				"half", "bottom",
				"open", "false",
				"powered", "false",
				"waterlogged", "false"), catalog.defaultProperties("minecraft:acacia_trapdoor"));
	}

	@Test
	void keepsColorBlocksAsBlockIdsWithoutProperties() {
		BlockStateCatalog catalog = BlockStateCatalog.latestJava();

		assertTrue(catalog.containsBlock("yellow_wool"));
		assertTrue(catalog.containsBlock("blue_wool"));
		assertTrue(catalog.properties("minecraft:yellow_wool").isEmpty());
		assertTrue(catalog.properties("minecraft:blue_wool").isEmpty());
	}

	@Test
	void indexLoadsAvailableCatalogsInDataVersionOrderAndSkipsMissingResources() {
		Map<String, String> resources = new HashMap<>();
		resources.put("old.json", "{\"version\":\"1.18.2\",\"dataVersion\":2865,\"source\":\"test\",\"blocks\":{}}");
		resources.put("new.json", "{\"version\":\"1.21.9\",\"dataVersion\":4554,\"source\":\"test\",\"blocks\":{}}");

		List<BlockStateCatalog> catalogs = BlockStateCatalog.loadCatalogs(
				new StringReader("[\"new.json\",\"missing.json\",\"old.json\"]"),
				path -> resources.containsKey(path) ? new StringReader(resources.get(path)) : null);

		assertEquals(List.of("1.18.2", "1.21.9"), catalogs.stream().map(BlockStateCatalog::version).toList());
	}
}
