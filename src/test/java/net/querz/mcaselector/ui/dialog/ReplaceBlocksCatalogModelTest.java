package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalog;
import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReplaceBlocksCatalogModelTest {

	@Test
	void defaultsToLatestAndAllowsManualCatalogSwitching() {
		BlockStateCatalog oldCatalog = catalog("1.18.2", 2975, "minecraft:stone");
		BlockStateCatalog latestCatalog = catalog("1.21.9", 4554, "minecraft:stone", "minecraft:resin_block");
		ReplaceBlocksCatalogModel model = new ReplaceBlocksCatalogModel(List.of(latestCatalog, oldCatalog));

		assertSame(latestCatalog, model.selected());
		assertTrue(model.select("1.18.2"));
		assertSame(oldCatalog, model.selected());
		assertFalse(model.selected().containsBlock("minecraft:resin_block"));
	}

	@Test
	void reportsUnknownIdsWithoutRejectingThem() {
		ReplaceBlocksCatalogModel model = new ReplaceBlocksCatalogModel(List.of(
				catalog("1.21.9", 4554, "minecraft:stone")));

		assertEquals(ReplaceBlocksCatalogModel.Compatibility.KNOWN,
				model.compatibility("minecraft:stone", false));
		assertEquals(ReplaceBlocksCatalogModel.Compatibility.UNKNOWN_SOURCE,
				model.compatibility("minecraft:future_block", true));
		assertEquals(ReplaceBlocksCatalogModel.Compatibility.UNKNOWN_TARGET,
				model.compatibility("example:custom_block", false));
	}

	@Test
	void exposesCompactCatalogLabelForTheBuilderToolbar() {
		BlockStateCatalog catalog = catalog("26.2", 4671, "minecraft:stone");
		ReplaceBlocksCatalogModel model = new ReplaceBlocksCatalogModel(List.of(catalog));

		assertEquals("Java 26.2", model.shortLabel(catalog));
		assertEquals("Java 26.2 (DataVersion 4671)", model.label(catalog));
	}

	private BlockStateCatalog catalog(String version, int dataVersion, String... names) {
		StringBuilder blocks = new StringBuilder();
		for (String name : names) {
			if (!blocks.isEmpty()) {
				blocks.append(',');
			}
			blocks.append('"').append(name).append("\":{\"properties\":{},\"defaultProperties\":{},\"stateCount\":1}");
		}
		return BlockStateCatalog.load(new StringReader("{\"version\":\"" + version
				+ "\",\"dataVersion\":" + dataVersion + ",\"source\":\"test\",\"blocks\":{" + blocks + "}}"));
	}
}
