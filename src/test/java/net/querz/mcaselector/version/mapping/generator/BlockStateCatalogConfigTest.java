package net.querz.mcaselector.version.mapping.generator;

import net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalog;
import net.querz.mcaselector.version.mapping.minecraft.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStateCatalogConfigTest {

	@Test
	void generatesCatalogWithReportPropertiesAndDefaultState(@TempDir Path temporaryDirectory) throws Exception {
		Path reportsFile = temporaryDirectory.resolve("blocks.json");
		Files.writeString(reportsFile, """
				{
				  "minecraft:test_block": {
				    "properties": {"facing": ["north", "south"]},
				    "states": [
				      {"id": 1, "default": true, "properties": {"facing": "north"}},
				      {"id": 2, "properties": {"facing": "south"}}
				    ]
				  }
				}
				""");
		Blocks reports = Blocks.load(reportsFile);

		BlockStateCatalog catalog = BlockStateCatalog.load(new StringReader(
				BlockStateCatalogConfig.toJson("1.99", 9999, reports)));

		assertEquals("1.99", catalog.version());
		assertEquals(9999, catalog.dataVersion());
		assertEquals(2, catalog.getBlock("minecraft:test_block").orElseThrow().stateCount());
		assertEquals("north", catalog.defaultProperties("minecraft:test_block").get("facing"));
		assertTrue(catalog.isValidPropertyValue("minecraft:test_block", "facing", "south"));
	}
}
