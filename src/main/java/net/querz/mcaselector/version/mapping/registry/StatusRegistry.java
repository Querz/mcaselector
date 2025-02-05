package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.querz.mcaselector.io.FileHelper;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatusRegistry {

	private StatusRegistry() {}

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private static final Map<String, String> valid = FileHelper.loadFromResource("mapping/registry/status.json", r -> {
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

	public static class StatusIdentifier implements Serializable {
		String name;
		String nameWithNamespace;
		boolean custom = false;

		public StatusIdentifier(String name) {
			if (name != null && name.startsWith("'") && name.endsWith("'")) {
				this.name = name.substring(1, name.length() - 1);
				custom = true;
			} else if (name != null && isValidName(name)) {
				initValid(name);
			} else {
				throw new IllegalArgumentException("invalid status");
			}
		}

		public StatusIdentifier(String name, boolean custom) {
			if (custom) {
				this.name = name;
				this.custom = true;
			} else if (isValidName(name)) {
				initValid(name);
			} else {
				throw new IllegalArgumentException("invalid status");
			}
		}

		private void initValid(String name) {
			if (name.startsWith("minecraft:")) {
				this.name = valid.get(name);
				this.nameWithNamespace = name;
			} else {
				this.name = name;
				this.nameWithNamespace = valid.get(name);
			}
		}

		public String getStatus() {
			return name;
		}

		public String getStatusWithNamespace() {
			if (custom) {
				return name;
			}
			return nameWithNamespace;
		}

		public boolean equals(String value) {
			if (value == null) {
				return false;
			}
			if (custom) {
				return name.equals(value);
			}
			return value.startsWith("minecraft:") && value.equals(nameWithNamespace) || value.equals(name);
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
