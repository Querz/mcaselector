package net.querz.mcaselector.headless;

import net.querz.mcaselector.exception.ParseException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ParamParser {

	private final String[] args;

	public ParamParser(String[] args) {
		this.args = args;
	}

	public Map<String, String> parse() throws IOException {
		Map<String, String> values = new HashMap<>();

		String currentKey = null;
		for (String s : args) {
			if (s.startsWith("--")) {
				if (values.containsKey(s)) {
					throw new ParseException("duplicate parameter " + s);
				}
				currentKey = s.substring(2);
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
}
