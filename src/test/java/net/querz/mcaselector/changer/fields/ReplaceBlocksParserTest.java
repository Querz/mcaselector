package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.version.ChunkFilter;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ReplaceBlocksParserTest {

	@Test
	void parsesOrderedRulesWithQuotedTargetTiles() {
		ReplaceBlocksParser.Result result = ReplaceBlocksParser.parse(
				"literal(stone)='example:target';{id:\"example:tile\"}, literal(dirt)=minecraft:gold_block");

		ReplaceBlocksParser.Success success = assertInstanceOf(ReplaceBlocksParser.Success.class, result);
		List<ReplaceBlocksParser.ParsedRule> rules = success.rules();
		assertEquals(2, rules.size());
		assertEquals("example:target", rules.get(0).target().getName());
		assertEquals("example:tile", rules.get(0).target().getTile().getString("id"));
		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, rules.get(1).source().getType());
	}

	@Test
	void acceptsSyntacticallyValidFutureVanillaIdsWithoutRegistryMembership() {
		ReplaceBlocksParser.Result result = ReplaceBlocksParser.parse(
				"literal(minecraft:future_block)=minecraft:future_target");

		ReplaceBlocksParser.Success success = assertInstanceOf(ReplaceBlocksParser.Success.class, result);
		assertEquals("minecraft:future_block", success.rules().get(0).source().getName());
		assertEquals("minecraft:future_target", success.rules().get(0).target().getName());
	}

	@Test
	void rejectsInvalidLegacyRegexBeforeExecution() {
		ReplaceBlocksParser.Result result = ReplaceBlocksParser.parse(
				"'minecraft:['=minecraft:stone");

		ReplaceBlocksParser.Failure failure = assertInstanceOf(ReplaceBlocksParser.Failure.class, result);
		assertEquals("INVALID_REGEX", failure.error().code().name());
		assertEquals(0, failure.error().offset());
	}

	@Test
	void reportsInvalidTargetTileSeparatelyFromInvalidTarget() {
		ReplaceBlocksParser.Result result = ReplaceBlocksParser.parse(
				"literal(minecraft:stone)=minecraft:barrel;not_snbt");

		ReplaceBlocksParser.Failure failure = assertInstanceOf(ReplaceBlocksParser.Failure.class, result);
		assertEquals(ReplaceBlocksParser.ErrorCode.INVALID_TILE, failure.error().code());
	}
}
