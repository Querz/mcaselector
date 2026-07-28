package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.changer.fields.ReplaceBlocksParser;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.mapping.registry.BlockRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;
import net.querz.nbt.io.snbt.ParseException;
import net.querz.nbt.io.snbt.SNBTParser;
import java.util.regex.Pattern;

final class ReplaceBlocksDiagnostics {

	private static final Pattern REGEX_META = Pattern.compile("[\\\\.^$|?*+()\\[\\]{}]");

	private ReplaceBlocksDiagnostics() {}

	static NameResult normalizeBuilderName(String raw, boolean source) {
		if (raw == null || raw.trim().isEmpty()) {
			return new NameResult(null, error(source
					? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_EMPTY.toString()
					: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_EMPTY.toString()));
		}
		String name = ReplaceBlocksParser.normalizeResourceLocation(stripOuterSingleQuotes(raw.trim()));
		if (name != null) {
			return new NameResult(name, none());
		}
		return new NameResult(null, error(source
				? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_FORMAT.toString()
				: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_FORMAT.toString()));
	}

	static NameResult normalizeBuilderValue(String raw, boolean source) {
		if (raw == null || raw.trim().isEmpty()) {
			return normalizeBuilderName(raw, source);
		}
		String value = raw.trim();
		if (!value.startsWith("{")) {
			return normalizeBuilderName(raw, source);
		}
		try {
			SNBTParser parser = new SNBTParser(value);
			Tag parsed = parser.parse(true);
			if (!(parsed instanceof CompoundTag state) || state.getString("Name") == null) {
				return invalidState(source);
			}
			return new NameResult(NBTUtil.toSNBT(state), none());
		} catch (ParseException | RuntimeException ex) {
			return invalidState(source);
		}
	}

	private static NameResult invalidState(boolean source) {
		return new NameResult(null, error(source
				? Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_STATE_SNBT.toString()
				: Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_STATE_SNBT.toString()));
	}

	static boolean parseReplaceBlocksValue(ReplaceBlocksField field, String value) {
		try {
			return field.parseNewValue(value);
		} catch (RuntimeException ex) {
			field.setNewValue(null);
			return false;
		}
	}

	static Diagnostic builderInvalid() {
		return error(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_INVALID.toString());
	}

	static Diagnostic builderDuplicate() {
		return error(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_DUPLICATE.toString());
	}

	static Diagnostic diagnoseValue(String raw, boolean parserAccepted) {
		if (raw == null || raw.trim().isEmpty()) {
			return none();
		}
		ReplaceBlocksParser.Result result = ReplaceBlocksParser.parse(raw);
		if (result instanceof ReplaceBlocksParser.Failure failure) {
			return parserError(failure.error());
		}
		if (!parserAccepted) {
			return builderInvalid();
		}
		for (ReplaceBlocksParser.ParsedRule rule : ((ReplaceBlocksParser.Success) result).rules()) {
			ChunkFilter.BlockReplaceSource source = rule.source();
			if (source.getType() == ChunkFilter.BlockReplaceSourceType.LEGACY_REGEX_NAME
					&& (REGEX_META.matcher(source.getName()).find()
					|| source.getName().startsWith("minecraft:") && !BlockRegistry.isValidName(source.getName()))) {
				return warning(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_REGEX.format(source.getName()));
			}
		}
		return none();
	}

	private static Diagnostic parserError(ReplaceBlocksParser.ParseError error) {
		return switch (error.code()) {
			case MISSING_EQUALS -> error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_MISSING_EQUALS.toString());
			case INVALID_REGEX -> error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_REGEX_SYNTAX.toString());
			case INVALID_TILE -> error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TILE_SNBT.toString());
			case TRAILING_INPUT -> error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TRAILING.toString());
			case INVALID_TARGET -> error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_TARGET_FORMAT.toString());
			case EMPTY_VALUE -> none();
			default -> error(Translation.DIALOG_REPLACE_BLOCKS_VALIDATION_SOURCE_MODE_WRAPPER.toString());
		};
	}

	static boolean needsQueryQuoteHint(String query) {
		if (query == null) {
			return false;
		}
		String fieldName = "ReplaceBlocks";
		int fieldIndex = query.indexOf(fieldName);
		if (fieldIndex < 0) {
			return false;
		}
		int equalsIndex = query.indexOf('=', fieldIndex + fieldName.length());
		if (equalsIndex < 0) {
			return false;
		}
		int valueStart = equalsIndex + 1;
		while (valueStart < query.length() && Character.isWhitespace(query.charAt(valueStart))) {
			valueStart++;
		}
		return valueStart < query.length() && query.charAt(valueStart) != '"';
	}

	private static String stripOuterSingleQuotes(String value) {
		if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	static Diagnostic none() {
		return new Diagnostic(Severity.NONE, "");
	}

	private static Diagnostic error(String message) {
		return new Diagnostic(Severity.ERROR, message);
	}

	private static Diagnostic warning(String message) {
		return new Diagnostic(Severity.WARNING, message);
	}

	enum Severity {
		NONE,
		ERROR,
		WARNING
	}

	record Diagnostic(Severity severity, String message) {
		boolean isNone() {
			return severity == Severity.NONE;
		}

		boolean isError() {
			return severity == Severity.ERROR;
		}

		boolean isWarning() {
			return severity == Severity.WARNING;
		}
	}

	record NameResult(String name, Diagnostic diagnostic) {}
}
