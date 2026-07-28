package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.changer.fields.ReplaceBlocksParser;
import net.querz.mcaselector.version.ChunkFilter;
import java.util.LinkedHashMap;
import java.util.Map;

final class ReplaceBlocksRuleBuilderModel {

	private ReplaceBlocksRuleBuilderModel() {}

	static Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> parseRules(String value) {
		ReplaceBlocksParser.Result result = ReplaceBlocksParser.parse(value);
		if (!(result instanceof ReplaceBlocksParser.Success success)) {
			return Map.of();
		}
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> rules = new LinkedHashMap<>();
		for (ReplaceBlocksParser.ParsedRule rule : success.rules()) {
			rules.put(rule.source(), rule.target());
		}
		return rules;
	}

	static String normalizeRulesValue(String value) {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> rules = parseRules(value);
		if (rules.isEmpty()) {
			return null;
		}
		ReplaceBlocksField field = new ReplaceBlocksField();
		field.setNewValue(rules);
		return field.valueToString();
	}
}
