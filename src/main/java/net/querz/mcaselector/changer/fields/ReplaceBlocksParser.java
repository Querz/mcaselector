package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.Tag;
import net.querz.nbt.io.snbt.ParseException;
import net.querz.nbt.io.snbt.SNBTParser;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parser for the complete ReplaceBlocks value grammar. Catalog membership is intentionally not
 * part of this class: parsing must accept syntactically valid future and modded resource IDs.
 */
public final class ReplaceBlocksParser {

	private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	private ReplaceBlocksParser() {}

	public static Result parse(String value) {
		if (value == null || value.trim().isEmpty()) {
			return failure(ErrorCode.EMPTY_VALUE, 0, "");
		}

		List<ParsedRule> rules = new ArrayList<>();
		String remaining = value.trim();
		int offset = value.indexOf(remaining);
		while (!remaining.isEmpty()) {
			SourceReadResult source;
			try {
				source = readSource(remaining);
			} catch (PatternSyntaxException ex) {
				return failure(ErrorCode.INVALID_REGEX, offset, remaining);
			}
			if (source == null) {
				return failure(ErrorCode.INVALID_SOURCE, offset, remaining);
			}
			String targetText = remaining.substring(source.read()).trim();
			int targetOffset = offset + source.read() + leadingWhitespace(remaining.substring(source.read()));
			if (!targetText.startsWith("=")) {
				return failure(ErrorCode.MISSING_EQUALS, targetOffset, targetText);
			}

			TargetReadResult target;
			try {
				target = readTarget(targetText.substring(1).trim());
			} catch (InvalidTileSyntaxException ex) {
				return failure(ErrorCode.INVALID_TILE, targetOffset + 1, targetText);
			}
			if (target == null) {
				return failure(ErrorCode.INVALID_TARGET, targetOffset + 1, targetText);
			}
			rules.add(new ParsedRule(source.source(), target.target()));

			String tail = target.tail().trim();
			if (tail.isEmpty()) {
				break;
			}
			if (!tail.startsWith(",")) {
				return failure(ErrorCode.TRAILING_INPUT, targetOffset + targetText.length() - tail.length(), tail);
			}
			remaining = tail.substring(1).trim();
			offset = value.length() - remaining.length();
		}
		return new Success(rules);
	}

	public static String normalizeResourceLocation(String raw) {
		if (raw == null) {
			return null;
		}
		String name = raw.trim();
		if (name.isEmpty()) {
			return null;
		}
		if (!name.contains(":")) {
			name = "minecraft:" + name;
		}
		return RESOURCE_LOCATION.matcher(name).matches() ? name : null;
	}

	private static TargetReadResult readTarget(String raw) {
		if (raw.isEmpty()) {
			return null;
		}
		int read;
		CompoundTag state = null;
		String name = null;
		if (raw.startsWith("{")) {
			try {
				SNBTParser parser = new SNBTParser(raw);
				Tag parsed = parser.parse(true);
				if (!(parsed instanceof CompoundTag compound) || compound.getString("Name") == null) {
					return null;
				}
				state = compound;
				read = parser.getReadChars() - 1;
			} catch (ParseException | RuntimeException ex) {
				return null;
			}
		} else if (raw.startsWith("'")) {
			int end = raw.indexOf('\'', 1);
			if (end <= 1) {
				return null;
			}
			name = raw.substring(1, end);
			read = end + 1;
		} else {
			int end = 0;
			while (end < raw.length() && raw.charAt(end) != ',' && raw.charAt(end) != ';') {
				end++;
			}
			name = normalizeResourceLocation(raw.substring(0, end));
			if (name == null) {
				return null;
			}
			read = end;
		}

		String tail = raw.substring(read).trim();
		CompoundTag tile = null;
		if (tail.startsWith(";")) {
			String tileText = tail.substring(1).trim();
			if (tileText.isEmpty()) {
				throw new InvalidTileSyntaxException();
			}
			try {
				SNBTParser parser = new SNBTParser(tileText);
				Tag parsed = parser.parse(true);
				if (!(parsed instanceof CompoundTag compound)) {
					throw new InvalidTileSyntaxException();
				}
				tile = compound;
				tail = tileText.substring(parser.getReadChars() - 1).trim();
			} catch (ParseException | RuntimeException ex) {
				throw new InvalidTileSyntaxException();
			}
		}

		ChunkFilter.BlockReplaceData target;
		if (name != null) {
			target = tile == null ? new ChunkFilter.BlockReplaceData(name) : new ChunkFilter.BlockReplaceData(name, tile);
		} else {
			target = tile == null ? new ChunkFilter.BlockReplaceData(state) : new ChunkFilter.BlockReplaceData(state, tile);
		}
		return new TargetReadResult(target, tail);
	}

	private static SourceReadResult readSource(String raw) {
		if (raw.startsWith("{")) {
			CompoundTag state = parseStateSelector(raw);
			if (state == null) {
				return null;
			}
			try {
				SNBTParser parser = new SNBTParser(raw);
				parser.parse(true);
				return new SourceReadResult(new ChunkFilter.BlockReplaceSource(state), parser.getReadChars() - 1);
			} catch (ParseException | RuntimeException ex) {
				return null;
			}
		}
		if (startsWithSourceWrapper(raw)) {
			return readWrappedSource(raw);
		}
		int equals = raw.indexOf('=');
		if (equals < 0) {
			return null;
		}
		String source = raw.substring(0, equals).trim();
		if (source.startsWith("'") && source.endsWith("'") && source.length() > 2) {
			source = source.substring(1, source.length() - 1);
		} else if (!source.startsWith("minecraft:")) {
			source = normalizeResourceLocation(source);
			if (source == null) {
				return null;
			}
		}
		return source.isEmpty() ? null : new SourceReadResult(new ChunkFilter.BlockReplaceSource(source), equals);
	}

	private static boolean startsWithSourceWrapper(String raw) {
		return raw.startsWith("literal(") || raw.startsWith("regex(") || raw.startsWith("props(")
				|| raw.startsWith("tile(") || raw.startsWith("no_tile(") || raw.startsWith("y(") || raw.startsWith("biome(");
	}

	private static SourceReadResult readWrappedSource(String raw) {
		if (raw.startsWith("biome(")) {
			return readBiomeWrapper(raw);
		}
		if (raw.startsWith("y(")) {
			return readYRangeWrapper(raw);
		}
		if (raw.startsWith("tile(")) {
			return readTileModeWrapper(raw, "tile(", ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY);
		}
		if (raw.startsWith("no_tile(")) {
			return readTileModeWrapper(raw, "no_tile(", ChunkFilter.BlockReplaceTileEntityMode.EXCLUDE_TILE_ENTITY);
		}
		if (raw.startsWith("literal(")) {
			return readNameWrapper(raw, "literal(", true);
		}
		if (raw.startsWith("regex(")) {
			return readNameWrapper(raw, "regex(", false);
		}
		if (raw.startsWith("props(")) {
			int end = findWrapperEnd(raw, "props(".length());
			if (end < 0) {
				return null;
			}
			CompoundTag state = parseStateSelector(unwrapWrapperArgument(raw.substring("props(".length(), end)));
			if (state == null || state.getCompoundTag("Properties") == null || state.getCompoundTag("Properties").isEmpty()) {
				return null;
			}
			return new SourceReadResult(ChunkFilter.BlockReplaceSource.selectedProperties(state), end + 1);
		}
		return null;
	}

	private static SourceReadResult readTileModeWrapper(String raw, String prefix, ChunkFilter.BlockReplaceTileEntityMode mode) {
		int end = findWrapperEnd(raw, prefix.length());
		if (end < 0) {
			return null;
		}
		SourceReadResult source = readSourceExpression(unwrapWrapperArgument(raw.substring(prefix.length(), end)));
		return source == null ? null : new SourceReadResult(source.source().withTileEntityMode(mode), end + 1);
	}

	private static SourceReadResult readYRangeWrapper(String raw) {
		int end = findWrapperEnd(raw, "y(".length());
		if (end < 0) {
			return null;
		}
		String argument = raw.substring("y(".length(), end).trim();
		int comma = argument.indexOf(',');
		if (comma < 0) {
			return null;
		}
		YRange range = parseYRange(argument.substring(0, comma));
		SourceReadResult source = range == null ? null : readSourceExpression(argument.substring(comma + 1));
		return source == null ? null : new SourceReadResult(source.source().withYRange(range.minY(), range.maxY()), end + 1);
	}

	private static SourceReadResult readBiomeWrapper(String raw) {
		int end = findWrapperEnd(raw, "biome(".length());
		if (end < 0) {
			return null;
		}
		String argument = raw.substring("biome(".length(), end).trim();
		int comma = argument.indexOf(',');
		if (comma < 0) {
			return null;
		}
		List<String> biomes = parseBiomeList(argument.substring(0, comma));
		SourceReadResult source = biomes == null ? null : readSourceExpression(argument.substring(comma + 1));
		return source == null ? null : new SourceReadResult(source.source().withBiomes(biomes), end + 1);
	}

	private static SourceReadResult readSourceExpression(String raw) {
		String source = raw.trim();
		if (source.isEmpty()) {
			return null;
		}
		SourceReadResult result = readSource(source + "=minecraft:stone");
		return result == null || result.read() != source.length() ? null : result;
	}

	private static SourceReadResult readNameWrapper(String raw, String prefix, boolean literal) {
		int end = findWrapperEnd(raw, prefix.length());
		if (end < 0) {
			return null;
		}
		String argument = unwrapWrapperArgument(raw.substring(prefix.length(), end));
		if (argument.isEmpty()) {
			return null;
		}
		if (literal) {
			String name = normalizeResourceLocation(argument);
			return name == null ? null : new SourceReadResult(ChunkFilter.BlockReplaceSource.literalName(name), end + 1);
		}
		return new SourceReadResult(ChunkFilter.BlockReplaceSource.regexName(argument), end + 1);
	}

	private static YRange parseYRange(String raw) {
		String[] parts = raw.trim().split("\\.\\.", -1);
		if (parts.length != 2 || parts[0].trim().isEmpty() && parts[1].trim().isEmpty()) {
			return null;
		}
		try {
			Integer minY = parts[0].trim().isEmpty() ? null : Integer.parseInt(parts[0].trim());
			Integer maxY = parts[1].trim().isEmpty() ? null : Integer.parseInt(parts[1].trim());
			return minY != null && maxY != null && minY > maxY ? null : new YRange(minY, maxY);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static List<String> parseBiomeList(String raw) {
		String[] parts = raw.trim().split(";", -1);
		List<String> biomes = new ArrayList<>(parts.length);
		for (String part : parts) {
			String biome = normalizeResourceLocation(part);
			if (biome == null || biome.startsWith("minecraft:") && !BiomeRegistry.isValidName(biome)) {
				return null;
			}
			biomes.add(biome);
		}
		return biomes.isEmpty() ? null : biomes;
	}

	private static CompoundTag parseStateSelector(String raw) {
		try {
			SNBTParser parser = new SNBTParser(raw);
			Tag parsed = parser.parse(true);
			return parsed instanceof CompoundTag state && state.getString("Name") != null ? state : null;
		} catch (ParseException | RuntimeException ex) {
			return null;
		}
	}

	private static int findWrapperEnd(String raw, int start) {
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean escaped = false;
		int nestedParens = 0;
		for (int i = start; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (escaped) {
				escaped = false;
				continue;
			}
			if (doubleQuoted && c == '\\') {
				escaped = true;
				continue;
			}
			if (c == '\'' && !doubleQuoted) {
				singleQuoted = !singleQuoted;
			} else if (c == '"' && !singleQuoted) {
				doubleQuoted = !doubleQuoted;
			} else if (c == '(' && !singleQuoted && !doubleQuoted) {
				nestedParens++;
			} else if (c == ')' && !singleQuoted && !doubleQuoted) {
				if (nestedParens == 0) {
					return i;
				}
				nestedParens--;
			}
		}
		return -1;
	}

	private static String unwrapWrapperArgument(String argument) {
		String trimmed = argument.trim();
		return trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")
				? trimmed.substring(1, trimmed.length() - 1)
				: trimmed;
	}

	private static int leadingWhitespace(String value) {
		int index = 0;
		while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
			index++;
		}
		return index;
	}

	private static Failure failure(ErrorCode code, int offset, String token) {
		return new Failure(new ParseError(code, Math.max(0, offset), token));
	}

	public sealed interface Result permits Success, Failure {}

	public record Success(List<ParsedRule> rules) implements Result {
		public Success {
			rules = List.copyOf(rules);
		}
	}

	public record Failure(ParseError error) implements Result {}

	public record ParsedRule(ChunkFilter.BlockReplaceSource source, ChunkFilter.BlockReplaceData target) {}

	public record ParseError(ErrorCode code, int offset, String token) {}

	public enum ErrorCode {
		EMPTY_VALUE, INVALID_SOURCE, INVALID_REGEX, MISSING_EQUALS, INVALID_TARGET, INVALID_TILE, TRAILING_INPUT
	}

	private record SourceReadResult(ChunkFilter.BlockReplaceSource source, int read) {}

	private record TargetReadResult(ChunkFilter.BlockReplaceData target, String tail) {}

	private static final class InvalidTileSyntaxException extends RuntimeException {}

	private record YRange(Integer minY, Integer maxY) {}
}
