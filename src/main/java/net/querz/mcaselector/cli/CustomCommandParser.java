package net.querz.mcaselector.cli;

import net.querz.mcaselector.exception.ParseException;
import net.querz.mcaselector.io.StringPointer;
import java.util.ArrayList;
import java.util.List;

public class CustomCommandParser {

	private final StringPointer ptr;

	public CustomCommandParser(String[] args) {
		ptr = new StringPointer(String.join(" ", args));
	}

	public String[] parse() throws ParseException {
		List<String> result = new ArrayList<>();
		ptr.skipWhitespace();
		while (ptr.hasNext()) {
			if (ptr.currentChar() == '"') {
				result.add(ptr.parseQuotedString());
			} else if (ptr.currentChar() == '\'') {
				result.add(ptr.parseQuotedString('\''));
			} else {
				result.add(ptr.parseSimpleString(c -> !Character.isWhitespace(c)));
			}
			ptr.skipWhitespace();
		}
		return result.toArray(new String[0]);
	}
}
