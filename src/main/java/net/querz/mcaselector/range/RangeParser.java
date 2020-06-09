package net.querz.mcaselector.range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RangeParser {

	private static final Pattern rangePattern = Pattern.compile("^(?<from>-?\\d*)(?<divider>:?)(?<to>-?\\d*)$");

	public static Range parseRange(String range) {

		// 1 --> single value range
		// :1 --> from negative infinity to 1
		// 1: --> from 1 to positive infinity
		// 1:4 --> from 1 to 4
		// : --> from negative infinity to positive infinity

		String trimmed = range.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		Matcher m = rangePattern.matcher(trimmed);
		if (m.find()) {
			String fromString = m.group("from");
			boolean divider = m.group("divider").isEmpty();
			String toString = m.group("to");

			int from, to;

			try {
				from = fromString.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(fromString);
				to = toString.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(toString);
			} catch (NumberFormatException ex) {
				return null;
			}

			if (divider) {
				return new Range(from, from);
			} else {
				return new Range(from, to);
			}
		}

		return null;
	}

	public static List<Range> parseRanges(String ranges, String delimiter) {
		if ("true".equals(ranges.trim())) {
			return new ArrayList<>(Collections.singletonList(new Range(Integer.MIN_VALUE, Integer.MAX_VALUE)));
		}

		String[] split = ranges.split(delimiter);
		List<Range> list = new ArrayList<>(split.length);
		for (String stringRange : split) {
			Range range = parseRange(stringRange);
			if (range == null) {
				return null;
			}

			list.add(range);
		}

		return list;
	}
}
