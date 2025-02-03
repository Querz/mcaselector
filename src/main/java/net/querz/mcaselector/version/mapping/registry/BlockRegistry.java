package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.StringPointer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BlockRegistry {

	private BlockRegistry() {}

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private static final Map<String, String> valid = FileHelper.loadFromResource("mapping/registry/blocks.json", r -> {
		Map<String, String> map = new HashMap<>();
		List<String> status = GSON.fromJson(r, new TypeToken<List<String>>(){}.getType());
		for (String s : status) {
			map.put(s, "minecraft:" + s);
			map.put("minecraft:" + s, s);
		}
		return map;
	});

	public static boolean isValidName(String name) {
		return valid.containsKey(name) || name != null && name.startsWith("'") && name.endsWith("'");
	}

	public static String[] parseBlockNames(String raw) {
		StringPointer sp = new StringPointer(raw);
		List<String> blocks = new ArrayList<>();
		try {
			while (sp.hasNext()) {
				sp.skipWhitespace();
				String rawName;
				if (sp.currentChar() == '\'') {
					rawName = "'" + sp.parseQuotedString('\'') + "'";
				} else {
					rawName = sp.parseSimpleString(BlockRegistry::isValidBlockChar);
				}

				String parsedName = parseBlockName(rawName);
				if (parsedName == null) {
					return null;
				}
				blocks.add(parsedName);
				sp.skipWhitespace();
				if (sp.hasNext()) {
					sp.expectChar(',');
					sp.skipWhitespace();
					if (!sp.hasNext()) {
						return null;
					}
				}
			}
		} catch (Exception ex) {
			return null;
		}
		return blocks.toArray(new String[0]);
	}

	private static boolean isValidBlockChar(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
				|| c >= '0' && c <= '9'
				|| c == ':' || c == '_' || c == ' ';
	}

	public static String parseBlockName(String raw) {
		raw = raw.replace(" ", "");
		if (valid.containsKey(raw)) {
			return raw;
		} else if (raw.startsWith("'") && raw.endsWith("'")) {
			return raw.substring(1, raw.length() - 1);
		}
		return null;
	}
}
