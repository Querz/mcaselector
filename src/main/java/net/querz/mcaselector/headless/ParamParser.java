package net.querz.mcaselector.headless;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ParamParser {

	private StringPointer ptr;

	public ParamParser(String[] args) {
		ptr = new StringPointer(String.join(" ", args));
	}

	public Map<String, String> read() throws IOException {
		Map<String, String> values = new HashMap<>();

		/*
		* -headless -key1 value -key2 "multiple values" -key3 \"escaped -key4 "\"escaped with quotes\""
		*
		*
		* */

		String currentKey = null;
		while (ptr.hasNext()) {
			String s = parseString();
			ptr.skipWhitespace();
			if (s.startsWith("-")) {
				if (values.containsKey(s)) {
					throw new ParseException("duplicate parameter " + s);
				}
				currentKey = s.substring(1);
				values.put(currentKey, null);
			} else {
				if (values.get(currentKey) != null) {
					throw new ParseException("multiple values for parameter " + currentKey);
				} else {
					values.put(currentKey, s);
				}
			}
		}
		return values;
	}

	private String parseString() throws IOException {
		if (ptr.currentChar() == '"') {
			return ptr.parseQuotedString();
		}
		return ptr.parseSimpleString();
	}
}
