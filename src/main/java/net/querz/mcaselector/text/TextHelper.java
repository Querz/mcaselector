package net.querz.mcaselector.text;

import net.querz.mcaselector.debug.Debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextHelper {

	private TextHelper() {}

	private static final Map<Pattern, Long> DURATION_REGEXP = new HashMap<>();

	static {
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:years?|y)"), 31536000L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:months?)"), 2628000L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:days?|d)"), 90000L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:hours?|h)"), 3600L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:minutes?|mins?)"), 60L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:seconds?|secs?|s)"), 1L);
	}

	public static long parseDuration(String d) {
		boolean result = false;
		int duration = 0;
		for (Map.Entry<Pattern, Long> entry : DURATION_REGEXP.entrySet()) {
			Matcher m = entry.getKey().matcher(d);
			if (m.find()) {
				duration += Long.parseLong(m.group("data")) * entry.getValue();
				result = true;
			}
		}
		if (!result) {
			throw new IllegalArgumentException("could not parse anything from duration string");
		}
		return duration;
	}

	private static final DateTimeFormatter TIMESTAMP_FORMAT =
			new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd[ [HH][:mm][:ss]]")
					.parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
					.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
					.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
					.toFormatter();

	private static final ZoneId ZONE_ID = ZoneId.systemDefault();

	public static int parseTimestamp(String t) {
		String trim = t.trim();
		try {
			LocalDateTime date = LocalDateTime.parse(trim, TIMESTAMP_FORMAT);
			ZonedDateTime zdt = ZonedDateTime.of(date, ZONE_ID);
			return (int) zdt.toInstant().getEpochSecond();
		} catch (DateTimeParseException e) {
			Debug.dump(e.getMessage());
		}
		throw new IllegalArgumentException("could not parse date time");
	}

	public static Integer parseInt(String s, int radix) {
		try {
			return Integer.parseInt(s, radix);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public static String byteToBinaryString(byte b) {
		StringBuilder s = new StringBuilder(Integer.toBinaryString(b & 0xFF));
		for (int i = s.length(); i < 8; i++) {
			s.insert(0, "0");
		}
		return s.toString();
	}

	public static String intToBinaryString(int n) {
		StringBuilder s = new StringBuilder(Integer.toBinaryString(n));
		for (int i = s.length(); i < 32; i++) {
			s.insert(0, "0");
		}
		return s.toString();
	}

	public static String longToBinaryString(long l) {
		StringBuilder s = new StringBuilder(Long.toBinaryString(l));
		for (int i = s.length(); i < 64; i++) {
			s.insert(0, "0");
		}
		return s.toString();
	}

	public static String getStacktraceAsString(Exception ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}
}
