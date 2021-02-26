package net.querz.mcaselector.changer;

import net.querz.mcaselector.exception.ParseException;
import net.querz.mcaselector.io.StringPointer;
import java.util.ArrayList;
import java.util.List;

public class ChangeParser {

	private final StringPointer ptr;

	public ChangeParser(String change) {
		ptr = new StringPointer(change);
	}

	public List<Field<?>> parse() throws ParseException {
		List<Field<?>> fields = new ArrayList<>();

		while (ptr.hasNext()) {
			ptr.skipWhitespace();
			// read key, operator, value
			String key = ptr.parseSimpleString(this::isValidCharacter);
			FieldType fieldType = FieldType.getByName(key);
			if (fieldType == null) {
				throw ptr.parseException("invalid field");
			}
			Field<?> field = fieldType.newInstance();
			if (field == null) {
				throw ptr.parseException("unable to create change field " + key);
			}

			ptr.skipWhitespace();

			ptr.expectChar('=');

			ptr.skipWhitespace();

			String value;
			if (ptr.currentChar() == '"') {
				value = ptr.parseQuotedString();
			} else {
				value = ptr.parseSimpleString(this::isValidCharacter);
			}

			if (!field.parseNewValue(value)) {
				throw ptr.parseException("invalid value");
			}

			ptr.skipWhitespace();

			//expect , if we didn't reach the end
			if (ptr.hasNext()) {
				ptr.expectChar(',');
			}

			fields.add(field);
		}
		return fields;
	}

	private boolean isValidCharacter(char c) {
		return c >= 'a' && c <= 'z'
				|| c >= 'A' && c <= 'Z'
				|| c >= '0' && c <= '9'
				|| c == '-'
				|| c == '+';
	}
}
