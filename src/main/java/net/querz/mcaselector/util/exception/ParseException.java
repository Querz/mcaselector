package net.querz.mcaselector.util.exception;

import java.io.IOException;

public class ParseException extends IOException {

	public ParseException(String msg) {
		super(msg);
	}

	public ParseException(String msg, String value, int index) {
		super(msg + " at: " + formatError(value, index));
	}

	private static String formatError(String value, int index) {
		StringBuilder builder = new StringBuilder();
		int i = Math.min(value.length(), index);
		if (i > 35) {
			builder.append("...");
		}
		builder.append(value, Math.max(0, i - 35), i);
		builder.append("<--[HERE]");
		return builder.toString();
	}
}
