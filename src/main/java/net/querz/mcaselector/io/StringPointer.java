package net.querz.mcaselector.io;

import net.querz.mcaselector.exception.ParseException;
import java.util.function.Function;

public class StringPointer {

	private final String value;
	private int index;

	public StringPointer(String value) {
		this.value = value;
	}

	public String parseSimpleString() {
		int oldIndex = index;
		while (hasNext() && !Character.isWhitespace(currentChar())) {
			index++;
		}
		return value.substring(oldIndex, index);
	}

	public String parseSimpleString(Function<Character, Boolean> valid) {
		int oldIndex = index;
		while (hasNext() && valid.apply(currentChar())) {
			index++;
		}
		return value.substring(oldIndex, index);
	}

	public String parseQuotedString() throws ParseException {
		return parseQuotedString('"');
	}

	public String parseQuotedString(char quote) throws ParseException {
		int oldIndex = ++index; // ignore beginning quotes
		StringBuilder sb = null;
		boolean escape = false;
		while (hasNext()) {
			char c = next();
			if (escape) {
				if (c != '\\' && c != quote) {
					throw parseException("invalid escape of '" + c + "'");
				}
				escape = false;
			} else {
				if (c == '\\') { // escape
					escape = true;
					if (sb != null) {
						continue;
					}
					sb = new StringBuilder(value.substring(oldIndex, index - 1));
					continue;
				}
				if (c == quote) {
					return sb == null ? value.substring(oldIndex, index - 1) : sb.toString();
				}
			}
			if (sb != null) {
				sb.append(c);
			}
		}
		throw parseException("missing end quote");
	}

	public boolean nextArrayElement() {
		skipWhitespace();
		if (hasNext() && currentChar() == ',') {
			index++;
			skipWhitespace();
			return true;
		}
		return false;
	}

	public void expectChar(char c) throws ParseException {
		skipWhitespace();
		boolean hasNext = hasNext();
		if (hasNext && currentChar() == c) {
			index++;
			return;
		}
		throw parseException("expected '" + c + "' but got " + (hasNext ? "'" + currentChar() + "'" : "EOF"));
	}

	public void expectString(String s) throws ParseException {
		skipWhitespace();
		int index = 0;
		while (hasNext() && index < s.length() && currentChar() == s.charAt(index)) {
			index++;
			this.index++;
		}
		if (index != s.length()) {
			throw parseException("expected \"" + s + "\" but got " + (hasNext() ? "\"" + value.substring(this.index - index, this.index) + "\"" : "EOF"));
		}
	}

	public void skipWhitespace() {
		while (hasNext() && Character.isWhitespace(currentChar())) {
			index++;
		}
	}

	public boolean hasNext() {
		return index < value.length();
	}

	public boolean hasCharsLeft(int num) {
		return this.index + num < value.length();
	}

	public char currentChar() {
		return value.charAt(index);
	}

	public int index() {
		return index;
	}

	public int size() {
		return value.length();
	}

	public char next() {
		return value.charAt(index++);
	}

	public void skip(int offset) {
		index += offset;
	}

	public char lookAhead(int offset) {
		return value.charAt(index + offset);
	}

	public ParseException parseException(String msg) {
		return new ParseException(msg, value, index);
	}
}
